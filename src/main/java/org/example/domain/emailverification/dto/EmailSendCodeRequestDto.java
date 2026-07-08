package org.example.domain.emailverification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class EmailSendCodeRequestDto {

    @Schema(description = "인증번호를 받을 이메일 주소", example = "user@example.com")
    @NotBlank
    @Email
    private String email;
}
