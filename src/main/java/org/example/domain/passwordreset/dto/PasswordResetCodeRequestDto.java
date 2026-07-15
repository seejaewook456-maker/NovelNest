package org.example.domain.passwordreset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordResetCodeRequestDto {

    @Schema(description = "비밀번호를 재설정할 가입 이메일 주소", example = "user@example.com")
    @NotBlank
    @Email
    private String email;
}
