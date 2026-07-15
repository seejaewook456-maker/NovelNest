package org.example.domain.passwordreset.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.passwordreset.dto.PasswordResetCodeRequestDto;
import org.example.domain.passwordreset.dto.PasswordResetConfirmRequestDto;
import org.example.domain.passwordreset.dto.PasswordResetVerifyRequestDto;
import org.example.domain.passwordreset.dto.PasswordResetVerifyResponseDto;
import org.example.domain.passwordreset.service.PasswordResetService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "비밀번호 재설정", description = "가입한 이메일로 인증번호를 받아 비밀번호를 재설정하는 API (AccessToken 불필요)")
@RestController
@RequestMapping("/api/auth/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String UNIFIED_CODE_SENT_MESSAGE = "입력하신 이메일로 가입된 계정이 있는 경우 인증번호가 발송됩니다.";

    private final PasswordResetService passwordResetService;

    @Operation(
            summary = "비밀번호 재설정 인증번호 발송",
            description = "가입한 이메일로 6자리 인증번호를 발송합니다. 계정 열거 공격을 막기 위해 이메일 미가입/소셜 로그인 전용 계정이어도 " +
                    "항상 동일한 성공 메시지를 반환하며, 실제 발송 여부는 노출하지 않습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "요청 접수(실제 발송 여부와 무관하게 항상 성공으로 응답)",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"입력하신 이메일로 가입된 계정이 있는 경우 인증번호가 발송됩니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429", description = "재전송 제한 시간 미경과",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"success\":false,\"code\":\"EMAIL_CODE_RESEND_TOO_SOON\",\"message\":\"인증번호는 60초 이후에 재전송할 수 있습니다.\"}"
                    ))
            )
    })
    @PostMapping("/code")
    public ResponseEntity<ApiResponse> sendCode(@Valid @RequestBody PasswordResetCodeRequestDto dto) {
        passwordResetService.sendCode(dto.getEmail());
        return ResponseEntity.ok(ApiResponse.of(UNIFIED_CODE_SENT_MESSAGE));
    }

    @Operation(
            summary = "비밀번호 재설정 인증번호 검증",
            description = "발송된 6자리 인증번호를 검증합니다. 성공 시 비밀번호 변경 API(/confirm)에서만 사용 가능한 " +
                    "일회성 임시 토큰(resetToken)을 발급합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "인증 성공, resetToken 발급",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"인증번호가 확인되었습니다.\",\"data\":{\"resetToken\":\"c3RyaW5n...\"}}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "인증번호 불일치 / 만료 / 발송 이력 없음",
                    content = @Content(examples = {
                            @ExampleObject(name = "인증번호 불일치", value = "{\"success\":false,\"code\":\"PASSWORD_RESET_CODE_MISMATCH\",\"message\":\"인증번호가 일치하지 않습니다.\"}"),
                            @ExampleObject(name = "인증번호 만료", value = "{\"success\":false,\"code\":\"PASSWORD_RESET_CODE_EXPIRED\",\"message\":\"인증번호가 만료되었습니다. 다시 요청해주세요.\"}")
                    })
            )
    })
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyCode(@Valid @RequestBody PasswordResetVerifyRequestDto dto) {
        PasswordResetVerifyResponseDto response = passwordResetService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(ApiResponse.of("인증번호가 확인되었습니다.", response));
    }

    @Operation(
            summary = "새 비밀번호로 변경",
            description = "인증번호 검증 성공 시 발급된 resetToken으로 새 비밀번호를 설정합니다. " +
                    "변경 완료 시 기존 RefreshToken은 즉시 무효화되어 다른 기기 세션도 재로그인이 필요합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "비밀번호 변경 성공",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"비밀번호가 변경되었습니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "재설정 토큰 만료 / 이미 사용됨 / 유효하지 않음 / 비밀번호 불일치",
                    content = @Content(examples = {
                            @ExampleObject(name = "토큰 만료", value = "{\"success\":false,\"code\":\"PASSWORD_RESET_TOKEN_EXPIRED\",\"message\":\"재설정 가능 시간이 만료되었습니다. 처음부터 다시 시도해주세요.\"}"),
                            @ExampleObject(name = "이미 사용된 토큰", value = "{\"success\":false,\"code\":\"PASSWORD_RESET_TOKEN_ALREADY_USED\",\"message\":\"이미 처리된 재설정 요청입니다. 처음부터 다시 시도해주세요.\"}"),
                            @ExampleObject(name = "비밀번호 불일치", value = "{\"success\":false,\"code\":\"PASSWORD_CONFIRM_MISMATCH\",\"message\":\"비밀번호와 비밀번호 확인이 일치하지 않습니다.\"}")
                    })
            )
    })
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse> confirm(@Valid @RequestBody PasswordResetConfirmRequestDto dto) {
        passwordResetService.confirmPassword(dto.getResetToken(), dto.getNewPassword(), dto.getNewPasswordConfirm());
        return ResponseEntity.ok(ApiResponse.of("비밀번호가 변경되었습니다."));
    }
}
