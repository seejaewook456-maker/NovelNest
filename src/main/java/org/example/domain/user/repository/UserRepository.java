package org.example.domain.user.repository;

import jakarta.persistence.LockModeType;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 활성(미탈퇴) 회원만 이메일로 조회 — 로그인/인증/회원가입 중복 확인 등 "현재 사용 가능한 계정"이 필요한 대부분의 조회에 사용
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    // AI 분당 Rate Limit(AiRateLimitService)의 "조회 후 기록" 구간을 같은 사용자끼리 직렬화하기 위한
    // 비관적 잠금(SELECT ... FOR UPDATE) 조회 — 활성 트랜잭션 안에서 호출해야 잠금이 유지된다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);

    // 활성(미탈퇴) 회원만 provider+providerId로 조회 — OAuth 로그인/가입 처리에 사용
    Optional<User> findByProviderAndProviderIdAndDeletedAtIsNull(Provider provider, String providerId);

    // 이메일 기준 가장 최근 탈퇴 이력 조회 — 재가입 제한 기간(설정값) 경과 여부 판단 전용
    Optional<User> findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(String email);

    // provider+providerId 기준 가장 최근 탈퇴 이력 조회 — OAuth 재가입 제한 기간 경과 여부 판단 전용
    Optional<User> findTopByProviderAndProviderIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Provider provider, String providerId);
}
