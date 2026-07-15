package org.example.domain.emailverification.service;

import org.example.domain.emailverification.entity.EmailVerification;
import org.example.domain.emailverification.entity.Purpose;
import org.example.domain.emailverification.repository.EmailVerificationRepository;
import org.example.domain.user.repository.UserRepository;
import org.example.global.event.EmailSendRequestedEvent;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    private ApplicationEventPublisher eventPublisher;

    private EmailVerificationService emailVerificationService;

    private static final String EMAIL = "user@example.com";

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationService(emailVerificationRepository, userRepository, eventPublisher);
    }

    @Test
    void 신규_이메일이면_인증번호를_발송하고_저장한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.empty());

        emailVerificationService.sendCode(EMAIL);

        verify(emailVerificationRepository).save(any(EmailVerification.class));
        verify(eventPublisher).publishEvent(any(EmailSendRequestedEvent.class));
    }

    @Test
    void 이미_가입된_이메일이면_발송을_거부한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> emailVerificationService.sendCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 재전송_요청이_60초_이내면_거부한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        EmailVerification recent = createVerification(EMAIL, "111111", Purpose.SIGN_UP, LocalDateTime.now().minusSeconds(10),
                LocalDateTime.now().plusMinutes(5));
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.of(recent));

        assertThatThrownBy(() -> emailVerificationService.sendCode(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_RESEND_TOO_SOON);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 인증번호가_일치하면_인증완료_처리한다() {
        EmailVerification verification = createVerification(EMAIL, "123456", Purpose.SIGN_UP, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.of(verification));

        emailVerificationService.verifyCode(EMAIL, "123456");

        assertThat(verification.isVerified()).isTrue();
    }

    @Test
    void 인증번호가_불일치하면_예외() {
        EmailVerification verification = createVerification(EMAIL, "123456", Purpose.SIGN_UP, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "000000"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_MISMATCH);
    }

    @Test
    void 인증번호가_만료되었으면_예외() {
        EmailVerification verification = createVerification(EMAIL, "123456", Purpose.SIGN_UP, LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().minusMinutes(5));
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_CODE_EXPIRED);
    }

    @Test
    void 인증완료_상태가_아니면_회원가입_검증을_통과하지_못한다() {
        EmailVerification verification = createVerification(EMAIL, "123456", Purpose.SIGN_UP, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.of(verification));

        assertThatThrownBy(() -> emailVerificationService.assertVerified(EMAIL))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void 인증완료_상태면_회원가입_검증을_통과한다() {
        EmailVerification verification = createVerification(EMAIL, "123456", Purpose.SIGN_UP, LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().plusMinutes(4));
        verification.markVerified(LocalDateTime.now());
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.SIGN_UP)).willReturn(Optional.of(verification));

        emailVerificationService.assertVerified(EMAIL);
    }

    @Test
    void 회원가입_완료후_인증정보를_삭제한다() {
        emailVerificationService.consume(EMAIL);

        verify(emailVerificationRepository).deleteByEmailAndPurpose(EMAIL, Purpose.SIGN_UP);
    }

    @Test
    void 목적이_다르면_같은_이메일이어도_서로_독립적으로_조회된다() {
        given(emailVerificationRepository.findByEmailAndPurpose(EMAIL, Purpose.PASSWORD_RESET)).willReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.verifyCode(EMAIL, "123456", Purpose.PASSWORD_RESET))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND);
    }

    private EmailVerification createVerification(String email, String code, Purpose purpose, LocalDateTime createdAt, LocalDateTime expiresAt) {
        return EmailVerification.create(email, code, purpose, createdAt, expiresAt);
    }
}
