package org.example.domain.passwordreset.dto;

import lombok.Getter;

@Getter
public class PasswordResetVerifyResponseDto {

    // 비밀번호 변경 API(/confirm)에서만 사용 가능한 일회성 임시 토큰. AccessToken/RefreshToken과 목적이 다르다.
    private final String resetToken;

    private PasswordResetVerifyResponseDto(String resetToken) {
        this.resetToken = resetToken;
    }

    public static PasswordResetVerifyResponseDto of(String resetToken) {
        return new PasswordResetVerifyResponseDto(resetToken);
    }
}
