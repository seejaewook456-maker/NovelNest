package org.example.domain.passwordreset.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private static final String EMAIL = "user@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "tokenExpirationMinutes", 10L);
        ReflectionTestUtils.setField(passwordResetService, "codeExpirationMinutes", 5L);
        ReflectionTestUtils.setField(passwordResetService, "emailSubject", "[NovelNest] 비밀번호 재설정 인증번호");
    }

    private User localUser() {
        return User.builder()
                .email(EMAIL)
                .password("encodedOldPassword")
                .nickname("홍길동")
                .provider(Provider.LOCAL)
                .build();
    }

    // ===== sendCode =====

    @Test
    void LOCAL_계정이면_인증번호를_발급하고_이메일_이벤트를_발행한다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(localUser()));
        given(emailVerificationService.issueCode(EMAIL, Purpose.PASSWORD_RESET)).willReturn("123456");

        passwordResetService.sendCode(EMAIL);

        verify(emailVerificationService).issueCode(EMAIL, Purpose.PASSWORD_RESET);
        verify(eventPublisher).publishEvent(any(EmailSendRequestedEvent.class));
    }

    @Test
    void 가입되지_않은_이메일이면_아무_일도_하지_않는다() {
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());

        passwordResetService.sendCode(EMAIL);

        verifyNoInteractions(emailVerificationService);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void 소셜_전용_계정이면_아무_일도_하지_않는다() {
        User googleUser = User.builder()
                .email(EMAIL)
                .nickname("홍길동")
                .provider(Provider.GOOGLE)
                .providerId("google-id")
                .build();
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(googleUser));

        passwordResetService.sendCode(EMAIL);

        verifyNoInteractions(emailVerificationService);
        verifyNoInteractions(eventPublisher);
    }

    // ===== verifyCode =====

    @Test
    void 인증번호_검증에_성공하면_인증정보를_소비하고_재설정_토큰을_발급한다() {
        // emailVerificationService.verifyCode는 void이며, 기본 Mockito 스텁 동작(아무 것도 하지 않음)이 성공 케이스와 동일하다.
        PasswordResetVerifyResponseDto response = passwordResetService.verifyCode(EMAIL, "123456");

        verify(emailVerificationService).verifyCode(EMAIL, "123456", Purpose.PASSWORD_RESET);
        verify(emailVerificationService).consume(EMAIL, Purpose.PASSWORD_RESET);
        verify(passwordResetTokenRepository).deleteByEmail(EMAIL);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        assertThat(response.getResetToken()).isNotBlank();
    }

    @Test
    void 인증번호_불일치는_비밀번호재설정_전용_에러코드로_변환된다() {
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.EMAIL_CODE_MISMATCH))
                .when(emailVerificationService).verifyCode(EMAIL, "000000", Purpose.PASSWORD_RESET);

        assertThatThrownBy(() -> passwordResetService.verifyCode(EMAIL, "000000"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_MISMATCH);

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void 인증번호_만료는_비밀번호재설정_전용_에러코드로_변환된다() {
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.EMAIL_CODE_EXPIRED))
                .when(emailVerificationService).verifyCode(EMAIL, "123456", Purpose.PASSWORD_RESET);

        assertThatThrownBy(() -> passwordResetService.verifyCode(EMAIL, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_EXPIRED);
    }

    @Test
    void 인증번호_발송이력이_없으면_비밀번호재설정_전용_에러코드로_변환된다() {
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND))
                .when(emailVerificationService).verifyCode(EMAIL, "123456", Purpose.PASSWORD_RESET);

        assertThatThrownBy(() -> passwordResetService.verifyCode(EMAIL, "123456"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_CODE_NOT_FOUND);
    }

    // ===== confirmPassword =====

    @Test
    void 새_비밀번호와_재입력이_다르면_예외() {
        assertThatThrownBy(() -> passwordResetService.confirmPassword("token", "newPw123", "different123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH);

        verifyNoInteractions(passwordResetTokenRepository);
    }

    @Test
    void 유효하지_않은_토큰이면_예외() throws Exception {
        given(passwordResetTokenRepository.findByTokenHash(sha256("bad-token"))).willReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.confirmPassword("bad-token", "newPw123", "newPw123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_INVALID);
    }

    @Test
    void 이미_사용된_토큰이면_예외() throws Exception {
        PasswordResetToken token = PasswordResetToken.create(EMAIL, sha256("used-token"), LocalDateTime.now(), LocalDateTime.now().plusMinutes(10));
        token.markUsed();
        given(passwordResetTokenRepository.findByTokenHash(sha256("used-token"))).willReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.confirmPassword("used-token", "newPw123", "newPw123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_ALREADY_USED);
    }

    @Test
    void 만료된_토큰이면_예외() throws Exception {
        PasswordResetToken token = PasswordResetToken.create(EMAIL, sha256("expired-token"), LocalDateTime.now().minusMinutes(20), LocalDateTime.now().minusMinutes(10));
        given(passwordResetTokenRepository.findByTokenHash(sha256("expired-token"))).willReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.confirmPassword("expired-token", "newPw123", "newPw123"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
    }

    @Test
    void 정상_토큰이면_비밀번호를_변경하고_리프레시토큰을_무효화하고_토큰을_사용처리한다() throws Exception {
        PasswordResetToken token = PasswordResetToken.create(EMAIL, sha256("good-token"), LocalDateTime.now(), LocalDateTime.now().plusMinutes(10));
        given(passwordResetTokenRepository.findByTokenHash(sha256("good-token"))).willReturn(Optional.of(token));
        User user = localUser();
        user.updateRefreshToken("someRefreshToken");
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(user));
        given(passwordEncoder.encode("newPw123")).willReturn("encodedNewPw");

        passwordResetService.confirmPassword("good-token", "newPw123", "newPw123");

        assertThat(user.getPassword()).isEqualTo("encodedNewPw");
        assertThat(user.getRefreshToken()).isNull();
        assertThat(token.isUsed()).isTrue();
    }

    // 서비스 내부 hash()와 동일한 로직으로 테스트용 해시를 만든다 (원문 토큰은 저장하지 않으므로 해시로만 조회 가능)
    private String sha256(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
