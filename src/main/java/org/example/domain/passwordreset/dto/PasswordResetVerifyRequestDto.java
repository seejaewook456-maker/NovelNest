package org.example.domain.passwordreset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class PasswordResetVerifyRequestDto {

    @Schema(description = "인증받은 이메일 주소", example = "user@example.com")
    @NotBlank
    @Email
    private String email;

    @Schema(description = "이메일로 발송된 6자리 인증번호", example = "123456")
    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리 숫자입니다.")
    private String code;
}
