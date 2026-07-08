package org.example.domain.emailverification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.emailverification.dto.EmailSendCodeRequestDto;
import org.example.domain.emailverification.dto.EmailVerifyCodeRequestDto;
import org.example.domain.emailverification.service.EmailVerificationService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이메일 인증", description = "일반 이메일 회원가입 전 이메일 소유 확인을 위한 인증번호 발송/검증")
@RestController
@RequestMapping("/api/users/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @Operation(
            summary = "이메일 인증번호 발송",
            description = "입력한 이메일로 6자리 인증번호를 발송합니다. 인증번호 유효시간은 5분이며, 동일 이메일은 60초 이내 재전송할 수 없습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "발송 성공",
                    content = @Content(examples = @ExampleObject(
                            name = "발송 성공",
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"인증번호가 이메일로 발송되었습니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(examples = @ExampleObject(
                            name = "이미 가입된 이메일",
                            value = "{\"success\":false,\"code\":\"EMAIL_ALREADY_REGISTERED\",\"message\":\"이미 가입된 이메일입니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "429", description = "재전송 제한 시간 미경과",
                    content = @Content(examples = @ExampleObject(
                            name = "재전송 제한",
                            value = "{\"success\":false,\"code\":\"EMAIL_CODE_RESEND_TOO_SOON\",\"message\":\"인증번호는 60초 이후에 재전송할 수 있습니다.\"}"
                    ))
            )
    })
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse> sendCode(@Valid @RequestBody EmailSendCodeRequestDto dto) {
        emailVerificationService.sendCode(dto.getEmail());
        return ResponseEntity.ok(ApiResponse.of("인증번호가 이메일로 발송되었습니다."));
    }

    @Operation(
            summary = "이메일 인증번호 검증",
            description = "발송된 6자리 인증번호를 검증합니다. 성공 시 해당 이메일은 인증 완료 상태가 되어 회원가입에 사용할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "인증 성공",
                    content = @Content(examples = @ExampleObject(
                            name = "인증 성공",
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"이메일 인증이 완료되었습니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "인증번호 불일치 / 만료 / 발송 이력 없음",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "인증번호 불일치",
                                    value = "{\"success\":false,\"code\":\"EMAIL_CODE_MISMATCH\",\"message\":\"인증번호가 일치하지 않습니다.\"}"
                            ),
                            @ExampleObject(
                                    name = "인증번호 만료",
                                    value = "{\"success\":false,\"code\":\"EMAIL_CODE_EXPIRED\",\"message\":\"인증번호가 만료되었습니다. 다시 요청해주세요.\"}"
                            )
                    })
            )
    })
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse> verifyCode(@Valid @RequestBody EmailVerifyCodeRequestDto dto) {
        emailVerificationService.verifyCode(dto.getEmail(), dto.getCode());
        return ResponseEntity.ok(ApiResponse.of("이메일 인증이 완료되었습니다."));
    }
}
