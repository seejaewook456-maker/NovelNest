package org.example.domain.emailverification.repository;

import org.example.domain.emailverification.entity.EmailVerification;
import org.example.domain.emailverification.entity.Purpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndPurpose(String email, Purpose purpose);

    void deleteByEmailAndPurpose(String email, Purpose purpose);

    // 만료된 인증번호 일괄 삭제 (목적 무관, 스케줄러 전용) — 개별 조회 없이 벌크 삭제로 처리
    @Modifying
    @Query("delete from EmailVerification e where e.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
