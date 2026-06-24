package org.example.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SignupRequestDto {

    @Schema(description = "이메일 주소", example = "user@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "비밀번호", example = "password123")
    @NotBlank
    private String password;

    @Schema(description = "닉네임", example = "홍길동")
    @NotBlank
    private String nickname;
}
