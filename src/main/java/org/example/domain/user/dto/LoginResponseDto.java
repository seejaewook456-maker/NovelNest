package org.example.domain.user.dto;

import lombok.Getter;

@Getter
public class LoginResponseDto {

    private final String accessToken;

    private LoginResponseDto(String accessToken) {
        this.accessToken = accessToken;
    }

    public static LoginResponseDto of(String accessToken) {
        return new LoginResponseDto(accessToken);
    }
}
