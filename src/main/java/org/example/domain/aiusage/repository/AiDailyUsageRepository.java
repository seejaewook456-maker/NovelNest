package org.example.domain.aiusage.repository;

import org.example.domain.aiusage.entity.AiDailyUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface AiDailyUsageRepository extends JpaRepository<AiDailyUsage, Long> {

    Optional<AiDailyUsage> findByUserIdAndUsageDate(Long userId, LocalDate usageDate);

    // 아래 5개는 기능별 카운트를 "제한 이내일 때만" 원자적으로 증가시키는 조건부 UPDATE다.
    // WHERE 절의 카운트 조건이 거짓이면 0행이 갱신되어 반환값이 0이 되므로, 애플리케이션 메모리에서
    // 조회 후 증가시키는 방식과 달리 동시 요청에서도 제한을 넘지 않는다
    // (같은 행에 대한 UPDATE는 DB 행 잠금으로 자연스럽게 직렬화된다).

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AiDailyUsage u SET u.summaryCount = u.summaryCount + 1 " +
            "WHERE u.userId = :userId AND u.usageDate = :usageDate AND u.summaryCount < :featureLimit")
    int incrementSummaryIfAllowed(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate,
                                   @Param("featureLimit") int featureLimit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AiDailyUsage u SET u.conflictCount = u.conflictCount + 1 " +
            "WHERE u.userId = :userId AND u.usageDate = :usageDate AND u.conflictCount < :featureLimit")
    int incrementConflictIfAllowed(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate,
                                    @Param("featureLimit") int featureLimit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AiDailyUsage u SET u.characterCount = u.characterCount + 1 " +
            "WHERE u.userId = :userId AND u.usageDate = :usageDate AND u.characterCount < :featureLimit")
    int incrementCharacterIfAllowed(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate,
                                     @Param("featureLimit") int featureLimit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AiDailyUsage u SET u.worldviewCount = u.worldviewCount + 1 " +
            "WHERE u.userId = :userId AND u.usageDate = :usageDate AND u.worldviewCount < :featureLimit")
    int incrementWorldviewIfAllowed(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate,
                                     @Param("featureLimit") int featureLimit);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE AiDailyUsage u SET u.chatCount = u.chatCount + 1 " +
            "WHERE u.userId = :userId AND u.usageDate = :usageDate AND u.chatCount < :featureLimit")
    int incrementChatIfAllowed(@Param("userId") Long userId, @Param("usageDate") LocalDate usageDate,
                                @Param("featureLimit") int featureLimit);
}
