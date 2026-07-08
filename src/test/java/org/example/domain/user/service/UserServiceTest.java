package org.example.domain.user.service;

import org.example.domain.emailverification.service.EmailVerificationService;
import org.example.domain.user.dto.SignupRequestDto;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService userService;

    private static final String EMAIL = "user@example.com";

    private SignupRequestDto signupRequest() {
        SignupRequestDto dto = new SignupRequestDto();
        ReflectionTestUtils.setField(dto, "email", EMAIL);
        ReflectionTestUtils.setField(dto, "password", "password123");
        ReflectionTestUtils.setField(dto, "nickname", "홍길동");
        return dto;
    }

    @Test
    void 이메일_인증을_완료하지_않으면_회원가입에_실패한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        doThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED))
                .when(emailVerificationService).assertVerified(EMAIL);

        assertThatThrownBy(() -> userService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_NOT_VERIFIED);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 이미_가입된_이메일이면_회원가입에_실패한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> userService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 이메일_인증을_완료했으면_회원가입에_성공하고_인증정보를_소비한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        doNothing().when(emailVerificationService).assertVerified(EMAIL);
        given(passwordEncoder.encode("password123")).willReturn("encoded");

        userService.signup(signupRequest());

        verify(userRepository).save(any(User.class));
        verify(emailVerificationService).consume(EMAIL);
    }
}
