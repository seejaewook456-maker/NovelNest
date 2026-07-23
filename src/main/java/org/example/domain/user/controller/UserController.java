package org.example.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.user.dto.LoginRequestDto;
import org.example.domain.user.dto.LoginResponseDto;
import org.example.domain.user.dto.SignupRequestDto;
import org.example.domain.user.dto.UserInfoResponseDto;
import org.example.domain.user.service.UserService;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "회원가입 / 로그인 / 내 정보 조회")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 닉네임으로 회원가입합니다. 사전에 /api/users/email/verify-code로 이메일 인증을 완료해야 합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "회원가입 성공",
                    content = @Content(examples = @ExampleObject(
                            name = "회원가입 성공",
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"회원가입 성공\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "잘못된 요청 (비밀번호 확인 불일치 또는 이메일 인증 미완료)",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "비밀번호 확인 불일치",
                                    value = "{\"success\":false,\"code\":\"PASSWORD_CONFIRM_MISMATCH\",\"message\":\"비밀번호와 비밀번호 확인이 일치하지 않습니다.\"}"
                            ),
                            @ExampleObject(
                                    name = "이메일 인증 미완료",
                                    value = "{\"success\":false,\"code\":\"EMAIL_NOT_VERIFIED\",\"message\":\"이메일 인증이 완료되지 않았습니다.\"}"
                            )
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 가입된 이메일",
                    content = @Content(examples = @ExampleObject(
                            name = "이미 가입된 이메일",
                            value = "{\"success\":false,\"code\":\"EMAIL_ALREADY_REGISTERED\",\"message\":\"이미 가입된 이메일입니다.\"}"
                    ))
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequestDto dto) {
        userService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회원가입 성공"));
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT accessToken을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequestDto dto) {
        LoginResponseDto response = userService.login(dto);
        return ResponseEntity.ok(ApiResponse.of("로그인 성공", response));
    }

    @Operation(summary = "내 정보 조회", description = "JWT 토큰으로 인증된 사용자의 정보를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UserInfoResponseDto response = userService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("내 정보 조회 성공", response));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "인증된 사용자 본인의 계정을 탈퇴 처리합니다(소프트 삭제). "
                    + "탈퇴 시각(deletedAt)만 기록하며 계정을 즉시 물리 삭제하지 않고, 작성한 작품·회차·등장인물·세계관 데이터도 삭제되지 않고 그대로 유지됩니다. "
                    + "탈퇴 이후에는 저장된 Refresh Token이 제거되어 재발급이 불가능하며, 기존 Access Token이 만료 전이더라도 더 이상 인증에 사용할 수 없습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "회원 탈퇴 성공",
                    content = @Content(examples = @ExampleObject(
                            name = "회원 탈퇴 성공",
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"회원 탈퇴가 완료되었습니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 실패 (Access Token 없음/만료/유효하지 않음, 또는 이미 탈퇴한 계정의 토큰)",
                    content = @Content(examples = @ExampleObject(
                            name = "인증 실패",
                            value = "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\"}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 탈퇴한 사용자",
                    content = @Content(examples = @ExampleObject(
                            name = "이미 탈퇴한 사용자",
                            value = "{\"success\":false,\"code\":\"USER_ALREADY_WITHDRAWN\",\"message\":\"이미 탈퇴한 사용자입니다.\"}"
                    ))
            )
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse> withdraw(@AuthenticationPrincipal UserDetails userDetails) {
        userService.withdraw(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("회원 탈퇴가 완료되었습니다."));
    }
}
