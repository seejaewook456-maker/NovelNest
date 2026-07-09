package org.example.domain.user.dto;

import lombok.Getter;

@Getter
public class TokenReissueResponseDto {

    private final String accessToken;
    private final String refreshToken;

    private TokenReissueResponseDto(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public static TokenReissueResponseDto of(String accessToken, String refreshToken) {
        return new TokenReissueResponseDto(accessToken, refreshToken);
    }
}
