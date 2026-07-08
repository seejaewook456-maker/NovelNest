package org.example.domain.emailverification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.emailverification.entity.EmailVerification;
import org.example.domain.emailverification.repository.EmailVerificationRepository;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${spring.mail.username:}")
    private String fromAddress;

    @Transactional
    public void sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = emailVerificationRepository.findByEmail(email).orElse(null);

        if (verification != null && verification.isResendTooSoon(now, RESEND_INTERVAL_SECONDS)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_RESEND_TOO_SOON);
        }

        String code = generateCode();
        LocalDateTime expiresAt = now.plusMinutes(EXPIRATION_MINUTES);

        if (verification == null) {
            emailVerificationRepository.save(EmailVerification.create(email, code, now, expiresAt));
        } else {
            verification.renew(code, now, expiresAt);
        }

        sendMail(email, code);
        log.info("Email verification code sent. email={}", email);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND));

        if (verification.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED);
        }
        if (!verification.matchesCode(code)) {
            throw new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH);
        }

        verification.markVerified(LocalDateTime.now());
        log.info("Email verified. email={}", email);
    }

    // 회원가입 시 이메일 인증 완료 여부 확인 (미인증이면 회원가입 불가)
    @Transactional(readOnly = true)
    public void assertVerified(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));
        if (!verification.isVerified()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
    }

    // 회원가입 완료 후 인증 정보 삭제 — 동일 인증번호 재사용 방지
    @Transactional
    public void consume(String email) {
        emailVerificationRepository.deleteByEmail(email);
    }

    private String generateCode() {
        return String.format("%0" + CODE_LENGTH + "d", secureRandom.nextInt(CODE_BOUND));
    }

    private void sendMail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(email);
            message.setSubject("[노벨네스트] 이메일 인증번호");
            message.setText("인증번호는 [" + code + "] 입니다. 5분 이내에 입력해주세요.");
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Failed to send verification email. email={}", email, e);
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }
}
