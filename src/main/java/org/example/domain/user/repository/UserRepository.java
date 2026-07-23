package org.example.domain.user.repository;

import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 회원가입 중복 검사용 — 탈퇴 여부와 무관하게 이메일 존재 자체를 확인한다 (탈퇴 이메일 즉시 재사용 방지)
    boolean existsByEmail(String email);

    // 탈퇴 회원 포함 전체 조회 — 탈퇴 여부를 직접 판별해야 하는 로그인/재발급 등 내부 로직 전용
    Optional<User> findByEmail(String email);

    // 정상(미탈퇴) 회원만 조회 — 인증된 사용자 판별(JWT 인증) 등 "활성 사용자"만 필요한 조회 전용
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);
}
