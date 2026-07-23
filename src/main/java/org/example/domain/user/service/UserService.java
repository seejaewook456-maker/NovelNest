package org.example.domain.user.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.emailverification.service.EmailVerificationService;
import org.example.domain.user.dto.LoginRequestDto;
import org.example.domain.user.dto.LoginResponseDto;
import org.example.domain.user.dto.SignupRequestDto;
import org.example.domain.user.dto.TokenReissueResponseDto;
import org.example.domain.user.dto.UserInfoResponseDto;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void signup(SignupRequestDto dto) {
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        // 일반 이메일 회원가입은 이메일 인증 완료 후에만 허용 (OAuth 회원가입은 이 경로를 타지 않음)
        emailVerificationService.assertVerified(dto.getEmail());

        User existing = userRepository.findByEmail(dto.getEmail()).orElse(null);
        if (existing != null) {
            // 탈퇴한 계정의 이메일은 재가입 기능이 없는 지금은 별도 문구로 명확히 구분해 안내한다.
            if (existing.isWithdrawn()) {
                throw new BusinessException(ErrorCode.WITHDRAWN_EMAIL_SIGNUP_BLOCKED);
            }
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        User user = User.builder()
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .nickname(dto.getNickname())
                .provider(Provider.LOCAL)
                .build();

        userRepository.save(user);
        emailVerificationService.consume(dto.getEmail()); // 인증번호 재사용 방지
        log.info("User registered. userId={}", user.getId());
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 탈퇴한 계정도 이메일/비밀번호 불일치와 동일한 응답으로 처리해 계정 존재·탈퇴 여부가 외부에 노출되지 않도록 하되,
        // 내부 로그에서는 탈퇴 계정의 로그인 시도임을 명확히 구분해 남긴다.
        if (user.isWithdrawn()) {
            log.info("Login attempt on withdrawn account. userId={}", user.getId());
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail());
        user.updateRefreshToken(refreshToken);
        log.info("User login success. userId={}", user.getId());
        return LoginResponseDto.of(accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public UserInfoResponseDto getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserInfoResponseDto.from(user);
    }

    // Refresh Token Rotation: 재발급 시마다 새 Refresh Token을 함께 발급해 저장된 값을 교체한다.
    // 탈취된 Refresh Token이 재사용되더라도, 정상 사용자가 먼저 재발급을 받으면 이전 토큰은 즉시 무효화된다.
    @Transactional
    public TokenReissueResponseDto reissue(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        String email;
        try {
            email = jwtTokenProvider.getEmail(refreshToken);
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 탈퇴한 계정은 재발급 대상에서 제외 — Refresh Token 검증 전에 먼저 확인해 탈퇴 계정의 토큰이
        // 우연히 살아있는 다른 회원의 저장값과 일치하는 경우까지 갈 필요 없이 즉시 차단한다.
        if (user.isWithdrawn()) {
            log.info("Token reissue attempt on withdrawn account. userId={}", user.getId());
            throw new BusinessException(ErrorCode.WITHDRAWN_USER);
        }

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);
        user.updateRefreshToken(newRefreshToken);

        log.info("Access token reissued. userId={}", user.getId());
        return TokenReissueResponseDto.of(newAccessToken, newRefreshToken);
    }

    // 회원 탈퇴: 소프트 삭제(deletedAt 기록) + Refresh Token 제거만 수행하며, 작품/회차 등 연관 데이터는
    // 이번 작업 범위에서 물리 삭제하지 않는다. 하나의 트랜잭션으로 처리된다.
    @Transactional
    public void withdraw(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.USER_ALREADY_WITHDRAWN);
        }

        user.withdraw();
        log.info("User withdrawn. userId={}", user.getId());
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.clearRefreshToken();
        log.info("User logout success. userId={}", user.getId());
    }
}
