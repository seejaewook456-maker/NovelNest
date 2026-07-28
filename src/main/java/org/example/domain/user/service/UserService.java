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
import org.example.domain.user.policy.UserRejoinPolicy;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.example.global.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final UserRejoinPolicy userRejoinPolicy;

    @Transactional
    public void signup(SignupRequestDto dto) {
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
        // 일반 이메일 회원가입은 이메일 인증 완료 후에만 허용 (OAuth 회원가입은 이 경로를 타지 않음)
        emailVerificationService.assertVerified(dto.getEmail());

        // 1. 활성 회원인지 확인
        if (userRepository.findByEmailAndDeletedAtIsNull(dto.getEmail()).isPresent()) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        // 2~3. 탈퇴 회원인지 확인 + 탈퇴 후 재가입 제한 기간이 지났는지 확인
        userRejoinPolicy.assertRejoinAllowed(
                userRepository.findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(dto.getEmail()));

        // 재가입인 경우에도 기존 User를 복구하지 않고 새 User를 생성한다.
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
        // 활성 회원이 없으면, 탈퇴 이력이 있는 이메일인지에 따라 안내 메시지를 구분한다.
        // (탈퇴 이력이 없는 경우는 존재하지 않는 계정/오타와 구분할 필요가 없어 비밀번호 불일치와 동일하게 처리한다)
        User user = userRepository.findByEmailAndDeletedAtIsNull(dto.getEmail()).orElse(null);
        if (user == null) {
            boolean everWithdrawn = userRepository
                    .findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(dto.getEmail())
                    .isPresent();
            if (everWithdrawn) {
                throw new BusinessException(ErrorCode.WITHDRAWN_ACCOUNT_LOGIN);
            }
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
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
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

        // 활성 회원만 재발급 대상 — 탈퇴한 계정은 탈퇴 이력 존재 여부로 구분해 명확한 에러를 반환한다.
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
        if (user == null) {
            Optional<User> withdrawnUser = userRepository.findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(email);
            if (withdrawnUser.isPresent()) {
                log.info("Token reissue attempt on withdrawn account. userId={}", withdrawnUser.get().getId());
                throw new BusinessException(ErrorCode.WITHDRAWN_USER);
            }
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
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
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElse(null);
        if (user == null) {
            boolean alreadyWithdrawn = userRepository.findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(email)
                    .isPresent();
            throw new BusinessException(alreadyWithdrawn ? ErrorCode.USER_ALREADY_WITHDRAWN : ErrorCode.USER_NOT_FOUND);
        }

        user.withdraw();
        log.info("User withdrawn. userId={}", user.getId());
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        user.clearRefreshToken();
        log.info("User logout success. userId={}", user.getId());
    }
}
