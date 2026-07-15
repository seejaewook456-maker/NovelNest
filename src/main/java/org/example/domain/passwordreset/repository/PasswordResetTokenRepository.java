package org.example.domain.passwordreset.repository;

import org.example.domain.passwordreset.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // 동일 이메일로 새 토큰을 발급할 때 이전에 남아있던 토큰을 무효화하기 위한 삭제
    void deleteByEmail(String email);

    // 만료되었거나 이미 사용된 토큰 일괄 삭제 (스케줄러 전용)
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now or t.used = true")
    int deleteExpiredOrUsed(@Param("now") LocalDateTime now);
}
