package org.example.domain.passwordreset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.emailverification.entity.Purpose;
import org.example.domain.emailverification.service.EmailVerificationService;
import org.example.domain.passwordreset.dto.PasswordResetVerifyResponseDto;
import org.example.domain.passwordreset.entity.PasswordResetToken;
import org.example.domain.passwordreset.repository.PasswordResetTokenRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.event.EmailSendRequestedEvent;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.token-expiration-minutes}")
    private long tokenExpirationMinutes;

    @Value("${app.password-reset.code-expiration-minutes}")
    private long codeExpirationMinutes;

    @Value("${app.password-reset.email-subject}")
    private String emailSubject;

    // 계정 열거 공격 방지를 위해 이 메서드는 미가입/소셜 전용 계정이어도 예외를 던지지 않고 조용히 종료한다.
    // 컨트롤러는 이 메서드의 실행 결과와 무관하게 항상 동일한 성공 메시지를 반환한다.
    @Transactional
    public void sendCode(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getProvider() != Provider.LOCAL) {
            log.info("Password reset code requested for non-resettable account. resettable=false");
            return;
        }

        String code = emailVerificationService.issueCode(email, Purpose.PASSWORD_RESET);

        eventPublisher.publishEvent(new EmailSendRequestedEvent(
                email,
                emailSubject,
                buildResetCodeEmailHtml(code),
                true
        ));
        log.info("Password reset code sent.");
    }

    @Transactional
    public PasswordResetVerifyResponseDto verifyCode(String email, String code) {
        try {
            emailVerificationService.verifyCode(email, code, Purpose.PASSWORD_RESET);
        } catch (BusinessException e) {
            throw translateVerificationError(e);
        }

        // 인증번호는 검증 성공 직후 즉시 소비(삭제)하고, 이후 비밀번호 변경까지는 별도 재설정 토큰으로 상태를 이어간다.
        emailVerificationService.consume(email, Purpose.PASSWORD_RESET);

        // 동일 이메일로 이전에 발급된 재설정 토큰이 남아있다면 새 토큰 발급과 동시에 무효화한다.
        passwordResetTokenRepository.deleteByEmail(email);

        String rawToken = generateRawToken();
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenExpirationMinutes);
        passwordResetTokenRepository.save(PasswordResetToken.create(email, tokenHash, LocalDateTime.now(), expiresAt));

        log.info("Password reset code verified, reset token issued.");
        return PasswordResetVerifyResponseDto.of(rawToken);
    }

    @Transactional
    public void confirmPassword(String rawResetToken, String newPassword, String newPasswordConfirm) {
        if (!newPassword.equals(newPasswordConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawResetToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));

        if (token.isUsed()) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);
        }
        if (token.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        User user = userRepository.findByEmail(token.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        user.updatePassword(passwordEncoder.encode(newPassword));
        // 비밀번호가 바뀌었으므로 다른 기기에 남아있는 로그인 세션도 재발급이 불가능하도록 즉시 무효화
        user.clearRefreshToken();
        token.markUsed();

        log.info("Password reset completed. userId={}", user.getId());
    }

    // 회원가입/재설정 인증번호는 같은 EmailVerificationService 예외를 공유하므로,
    // 비밀번호 재설정 API 응답에서는 목적에 맞는 전용 에러코드로 변환해 반환한다.
    private BusinessException translateVerificationError(BusinessException e) {
        if (e.getErrorCode() == ErrorCode.EMAIL_VERIFICATION_NOT_FOUND) {
            return new BusinessException(ErrorCode.PASSWORD_RESET_CODE_NOT_FOUND);
        }
        if (e.getErrorCode() == ErrorCode.EMAIL_CODE_MISMATCH) {
            return new BusinessException(ErrorCode.PASSWORD_RESET_CODE_MISMATCH);
        }
        if (e.getErrorCode() == ErrorCode.EMAIL_CODE_EXPIRED) {
            return new BusinessException(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);
        }
        return e;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }

    // Warm Brown/Cream 톤의 브랜드 이메일. 인증번호 외 비밀번호, 토큰 등 민감정보는 절대 포함하지 않는다.
    private String buildResetCodeEmailHtml(String code) {
        return """
                <div style="font-family: -apple-system, sans-serif; background-color: #FAF8F5; padding: 32px;">
                  <div style="max-width: 480px; margin: 0 auto; background-color: #FFFFFF; border: 1px solid #E7DDD3; border-radius: 12px; padding: 32px;">
                    <p style="color: #8B5E3C; font-weight: bold; font-size: 14px; margin: 0 0 16px;">노벨네스트</p>
                    <h2 style="color: #2F2A26; margin: 0 0 16px;">비밀번호 재설정 인증번호</h2>
                    <p style="color: #2F2A26; line-height: 1.6;">
                      비밀번호 재설정을 요청하셨습니다. 아래 인증번호를 입력해주세요.
                    </p>
                    <p style="font-size: 28px; font-weight: bold; letter-spacing: 4px; color: #70492E; background-color: #F5EFE8; padding: 16px 24px; border-radius: 8px; text-align: center;">
                      %s
                    </p>
                    <p style="color: #2F2A26; line-height: 1.6;">
                      이 인증번호는 %d분 후에 만료됩니다.
                    </p>
                    <p style="color: #8a7f76; font-size: 13px; line-height: 1.6;">
                      본인이 요청하지 않았다면 이 이메일을 무시해주세요.<br/>
                      인증번호는 타인에게 공유하지 마세요.
                    </p>
                  </div>
                </div>
                """.formatted(code, codeExpirationMinutes);
    }
}
