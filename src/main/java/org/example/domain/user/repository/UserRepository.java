package org.example.domain.user.repository;

import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 활성(미탈퇴) 회원만 이메일로 조회 — 로그인/인증/회원가입 중복 확인 등 "현재 사용 가능한 계정"이 필요한 대부분의 조회에 사용
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    // 활성(미탈퇴) 회원만 provider+providerId로 조회 — OAuth 로그인/가입 처리에 사용
    Optional<User> findByProviderAndProviderIdAndDeletedAtIsNull(Provider provider, String providerId);

    // 이메일 기준 가장 최근 탈퇴 이력 조회 — 재가입 제한 기간(설정값) 경과 여부 판단 전용
    Optional<User> findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(String email);

    // provider+providerId 기준 가장 최근 탈퇴 이력 조회 — OAuth 재가입 제한 기간 경과 여부 판단 전용
    Optional<User> findTopByProviderAndProviderIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Provider provider, String providerId);
}
