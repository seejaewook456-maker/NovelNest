package org.example.domain.user.controller;

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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody SignupRequestDto dto) {
        userService.signup(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of("회원가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequestDto dto) {
        LoginResponseDto response = userService.login(dto);
        return ResponseEntity.ok(ApiResponse.of("로그인 성공", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        UserInfoResponseDto response = userService.getMe(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("내 정보 조회 성공", response));
    }
}
