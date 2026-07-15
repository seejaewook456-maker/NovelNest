package org.example.domain.passwordreset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordResetConfirmRequestDto {

    @Schema(description = "인증번호 검증 성공 시 발급받은 임시 재설정 토큰")
    @NotBlank
    private String resetToken;

    @Schema(description = "새 비밀번호", example = "newPassword123")
    @NotBlank
    private String newPassword;

    @Schema(description = "새 비밀번호 확인", example = "newPassword123")
    @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    private String newPasswordConfirm;
}
