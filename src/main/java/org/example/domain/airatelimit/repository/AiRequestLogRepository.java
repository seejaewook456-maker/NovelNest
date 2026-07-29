package org.example.domain.airatelimit.repository;

import org.example.domain.airatelimit.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, Long> {

    // windowStart 이후(초과, exclusive)에 기록된 해당 사용자의 요청 개수 — 분당 Rate Limit(Sliding Window) 판단에 사용.
    long countByUserIdAndRequestedAtAfter(Long userId, LocalDateTime windowStart);

    // Sliding Window 밖으로 나가 더 이상 어떤 판단에도 쓰이지 않는 오래된 로그를 정리한다.
    // 별도 배치/스케줄러 없이, Rate Limit 체크가 일어날 때마다 함께 청소해 테이블이 무한히 커지지 않게 한다
    // (AiRateLimitService 참고).
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AiRequestLog r WHERE r.requestedAt < :cutoff")
    int deleteByRequestedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
