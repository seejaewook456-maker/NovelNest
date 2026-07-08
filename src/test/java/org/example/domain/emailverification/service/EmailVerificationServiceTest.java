package org.example.domain.emailverification.service;

import org.example.domain.emailverification.entity.EmailVerification;
import org.example.domain.emailverification.repository.EmailVerificationRepository;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationRepository emailVerificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    private EmailVerificationService emailVerificationService;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(emailVerificationRepository, userRepository, mailSender);
        ReflectionTestUtils.setField(emailVerificationService, "fromAddress", "no-reply@novelnestia.com");
    }

    @Test
    void 신규_이메일이면_인증번호를_발송하고_저장한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        emailVerificationService.sendCode(EMAIL);

        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void 이미_가입된_이메일이면_발송을_거부한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

        verifyNoInteractions(mailSender);
    }

    @Test
    void 재전송_요청이_60초_이내면_거부한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        EmailVerification recent = createVerification(EMAIL, "111111", LocalDateTime.now().minusSeconds(10),
                LocalDateTime.now().plusMinutes(5));
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.of(recent));

        assertThatThrownBy(() -> emailVerificationService.sendCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_RESEND_TOO_SOON);

        verifyNoInteractions(mailSender);
    }

    @Test
    void 인증번호가_일치하면_인증완료_처리한다() {
        EmailVerification verification = createVerification(EMAIL, "123456", LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.of(verification));

        emailVerificationService.verifyCode(EMAIL, "123456");

        assertThat(verification.isVerified()).isTrue();
    }

    @Test
    void 인증번호가_불일치하면_예외() {
        EmailVerification verification = createVerification(EMAIL, "123456", LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "000000"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_MISMATCH);
    }

    @Test
    void 인증번호가_만료되었으면_예외() {
        EmailVerification verification = createVerification(EMAIL, "123456", LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(5));
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED);
    }

    @Test
    void 인증완료_상태가_아니면_회원가입_검증을_통과하지_못한다() {
        EmailVerification verification = createVerification(EMAIL, "123456", LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.assertVerified(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void 인증완료_상태면_회원가입_검증을_통과한다() {
        EmailVerification verification = createVerification(EMAIL, "123456", LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        verification.markVerified(LocalDateTime.now());
        given(emailVerificationRepository.findByEmail(EMAIL)).willReturn(Optional.of(verification));

        emailVerificationService.assertVerified(EMAIL);
    }

    @Test
    void 회원가입_완료후_인증정보를_삭제한다() {
        emailVerificationService.consume(EMAIL);

        verify(emailVerificationRepository).deleteByEmail(EMAIL);
    }

    private EmailVerification createVerification(String email, String code, LocalDateTime createdAt, LocalDateTime expiresAt) {
        return EmailVerification.create(email, code, createdAt, expiresAt);
    }
}
