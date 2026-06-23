package org.example.domain.user.dto;

import lombok.Getter;
import org.example.domain.user.entity.User;

@Getter
public class UserInfoResponseDto {

    private final Long id;
    private final String email;
    private final String nickname;

    private UserInfoResponseDto(Long id, String email, String nickname) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
    }

    public static UserInfoResponseDto from(User user) {
        return new UserInfoResponseDto(user.getId(), user.getEmail(), user.getNickname());
    }
}
