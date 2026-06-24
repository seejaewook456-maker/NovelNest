package org.example.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입합니다.")
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
}
