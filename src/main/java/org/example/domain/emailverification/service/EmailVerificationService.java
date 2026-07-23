package org.example.domain.emailverification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.emailverification.entity.EmailVerification;
import org.example.domain.emailverification.entity.Purpose;
import org.example.domain.emailverification.repository.EmailVerificationRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.event.EmailSendRequestedEvent;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_BOUND = 1_000_000; // 10^CODE_LENGTH
    private static final long EXPIRATION_MINUTES = 5;
    private static final long RESEND_INTERVAL_SECONDS = 60;

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    // 회원가입 이메일 인증번호 발송 (기존 호출부 호환용 — 내부적으로 SIGN_UP 목적으로 위임)
    @Transactional
    public void sendCode(String email) {
        User existing = userRepository.findByEmail(email).orElse(null);
        if (existing != null) {
            // 탈퇴한 계정의 이메일은 재가입 기능이 없는 지금은 별도 문구로 명확히 구분해 안내한다.
            if (existing.isWithdrawn()) {
                throw new BusinessException(ErrorCode.WITHDRAWN_EMAIL_SIGNUP_BLOCKED);
            }
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        String code = issueCode(email, Purpose.SIGN_UP);
        sendSignUpMail(email, code);
    }

    // 목적(SIGN_UP/PASSWORD_RESET)별로 독립된 인증번호를 발급하고 생성된 코드를 반환한다.
    // 메일 발송 여부/형식(평문 vs HTML)은 목적마다 다르므로 발송은 호출자가 책임진다.
    @Transactional
    public String issueCode(String email, Purpose purpose) {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository.findByEmailAndPurpose(email, purpose).orElse(null);

        if (verification != null && verification.isResendTooSoon(now, RESEND_INTERVAL_SECONDS)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_RESEND_TOO_SOON);
        }

        String code = generateCode();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRATION_MINUTES);

        if (verification == null) {
            emailVerificationRepository.save(EmailVerification.create(email, code, purpose, now, expiresAt));
        } else {
            verification.renew(code, now, expiresAt);
        }

        log.info("Email verification code issued. purpose={}", purpose);
        return code;
    }

    @Transactional
    public void verifyCode(String email, String code) {
        verifyCode(email, code, Purpose.SIGN_UP);
    }

    @Transactional
    public void verifyCode(String email, String code, Purpose purpose) {
        EmailVerification verification = emailVerificationRepository.findByEmailAndPurpose(email, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (verification.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (!verification.matchesCode(code)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH);
        }

        verification.markVerified(LocalDateTime.now());
        log.info("Email verified. purpose={}", purpose);
    }

    // 회원가입 시 이메일 인증 완료 여부 확인 (미인증이면 회원가입 불가)
    @Transactional(readOnly = true)
    public void assertVerified(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmailAndPurpose(email, Purpose.SIGN_UP)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));
        if (!verification.isVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    // 회원가입 완료 후 인증 정보 삭제 — 동일 인증번호 재사용 방지
    @Transactional
    public void consume(String email) {
        consume(email, Purpose.SIGN_UP);
    }

    // 목적별 인증 정보 삭제 — 인증 성공 직후 즉시 소비해 동일 인증번호 재사용을 막는다
    @Transactional
    public void consume(String email, Purpose purpose) {
        emailVerificationRepository.deleteByEmailAndPurpose(email, purpose);
    }

    private String generateCode() {
        return String.format("%0" + CODE_LENGTH + "d", secureRandom.nextInt(CODE_BOUND));
    }

    // 발송 자체는 트랜잭션 커밋 이후 비동기로 처리되도록 이벤트만 발행 (EmailSendEventListener 참고)
    private void sendSignUpMail(String email, String code) {
        eventPublisher.publishEvent(new EmailSendRequestedEvent(
                email,
                "[노벨네스트] 이메일 인증번호",
                buildSignUpCodeEmailHtml(code),
                true
        ));
    }

    // Warm Brown/Cream 톤의 브랜드 이메일 (PasswordResetService의 비밀번호 재설정 메일과 동일한 스타일)
    private String buildSignUpCodeEmailHtml(String code) {
        return """
                <div style="font-family: -apple-system, sans-serif; background-color: #FAF8F5; padding: 32px;">
                  <div style="max-width: 480px; margin: 0 auto; background-color: #FFFFFF; border: 1px solid #E7DDD3; border-radius: 12px; padding: 32px;">
                    <p style="color: #8B5E3C; font-weight: bold; font-size: 14px; margin: 0 0 16px;">노벨네스트</p>
                    <h2 style="color: #2F2A26; margin: 0 0 16px;">이메일 인증번호</h2>
                    <p style="color: #2F2A26; line-height: 1.6;">
                      회원가입을 위해 이메일 인증을 요청하셨습니다. 아래 인증번호를 입력해주세요.
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
                """.formatted(code, EXPIRATION_MINUTES);
    }
}
