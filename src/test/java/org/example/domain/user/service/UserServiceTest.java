package org.example.domain.user.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.example.domain.emailverification.service.EmailVerificationService;
import org.example.domain.user.dto.SignupRequestDto;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
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
    private static final String PASSWORD = "password123";

    private SignupRequestDto signupRequest() {
        SignupRequestDto dto = new SignupRequestDto();
        ReflectionTestUtils.setField(dto, "email", EMAIL);
        ReflectionTestUtils.setField(dto, "password", PASSWORD);
        ReflectionTestUtils.setField(dto, "passwordConfirm", PASSWORD);
        ReflectionTestUtils.setField(dto, "nickname", "홍길동");
        return dto;
    }

    @Test
    void 비밀번호와_비밀번호확인이_다르면_회원가입에_실패한다() {
        SignupRequestDto dto = signupRequest();
        ReflectionTestUtils.setField(dto, "passwordConfirm", "otherPassword123");

        assertThatThrownBy(() -> userService.signup(dto))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PASSWORD_CONFIRM_MISMATCH);

        verify(userRepository, never()).save(any(User.class));
        verify(emailVerificationService, never()).assertVerified(EMAIL);
    }

    @Test
    void passwordConfirm이_비어있으면_검증에_실패한다() {
        SignupRequestDto dto = signupRequest();
        ReflectionTestUtils.setField(dto, "passwordConfirm", "");

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<SignupRequestDto>> violations = validator.validate(dto);

        assertThat(violations)
                .anyMatch(v -> v.getPropertyPath().toString().equals("passwordConfirm"));
    }

    @Test
    void 이메일_인증을_완료하지_않으면_회원가입에_실패한다() {
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
        doNothing().when(emailVerificationService).assertVerified(EMAIL);
        given(userRepository.existsByEmail(EMAIL)).willReturn(true);

        assertThatThrownBy(() -> userService.signup(signupRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 비밀번호가_일치하고_이메일_인증을_완료했으면_회원가입에_성공하고_인증정보를_소비한다() {
        given(userRepository.existsByEmail(EMAIL)).willReturn(false);
        doNothing().when(emailVerificationService).assertVerified(EMAIL);
        given(passwordEncoder.encode(PASSWORD)).willReturn("encoded");

        userService.signup(signupRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(emailVerificationService).consume(EMAIL);

        // passwordConfirm은 암호화 대상도, 저장 대상도 아니며 password만 인코딩되어 저장된다
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded");
    }
}
