package org.example.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.domain.user.dto.RefreshTokenRequestDto;
import org.example.domain.user.dto.TokenReissueResponseDto;
import org.example.domain.user.service.UserService;
import org.example.global.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증", description = "Access Token 재발급 / 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token으로 새 Access Token을 재발급합니다. Refresh Token Rotation 정책에 따라 새 Refresh Token도 함께 발급되며, " +
                    "이전 Refresh Token은 즉시 무효화됩니다. Access Token 만료로 401을 받은 프론트가 자동으로 호출하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "재발급 성공",
                    content = @Content(examples = @ExampleObject(
                            name = "재발급 성공",
                            value = "{\"success\":true,\"code\":\"OK\",\"message\":\"토큰 재발급 성공\",\"data\":{\"accessToken\":\"...\",\"refreshToken\":\"...\"}}"
                    ))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Refresh Token 없음 / 만료 / 유효하지 않음 / 저장된 값과 불일치",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "Refresh Token 만료",
                                    value = "{\"success\":false,\"code\":\"REFRESH_TOKEN_EXPIRED\",\"message\":\"Refresh Token이 만료되었습니다. 다시 로그인해주세요.\"}"
                            ),
                            @ExampleObject(
                                    name = "저장된 값과 불일치",
                                    value = "{\"success\":false,\"code\":\"REFRESH_TOKEN_MISMATCH\",\"message\":\"저장된 Refresh Token과 일치하지 않습니다.\"}"
                            )
                    })
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "사용자를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "사용자 없음",
                            value = "{\"success\":false,\"code\":\"USER_NOT_FOUND\",\"message\":\"사용자를 찾을 수 없습니다.\"}"
                    ))
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(@Valid @RequestBody RefreshTokenRequestDto dto) {
        TokenReissueResponseDto response = userService.reissue(dto.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.of("토큰 재발급 성공", response));
    }

    @Operation(
            summary = "로그아웃",
            description = "DB에 저장된 Refresh Token을 무효화합니다. 로그아웃 이후에는 기존 Refresh Token으로 Access Token을 재발급할 수 없습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.logout(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.of("로그아웃 성공"));
    }
}
