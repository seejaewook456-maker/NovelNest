package org.example.domain.aiusage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.aiusage.config.AiUsageLimitProperties;
import org.example.domain.aiusage.dto.AiDailyUsageResponseDto;
import org.example.domain.aiusage.dto.AiUsageDetailDto;
import org.example.domain.aiusage.entity.AiDailyUsage;
import org.example.domain.aiusage.enums.AiFeatureType;
import org.example.domain.aiusage.repository.AiDailyUsageRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

// AI 기능 하루 사용량을 검사·증가·조회하는 단일 창구.
// 각 AI 서비스(EpisodeSummaryService, ChatService 등)는 OpenAI 호출 직전에 checkAndIncrement()만
// 호출하면 되고, 제한값 판단·원자적 증가·예외 분기 로직은 전부 이 클래스 하나에 모여 있다.
// 전체 합계 제한은 두지 않고, 기능별 제한만 적용한다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiUsageService {

    private final AiDailyUsageRepository aiDailyUsageRepository;
    private final AiUsageLimitProperties limitProperties;
    private final UserRepository userRepository;
    private final Clock clock;
    private final AiDailyUsageRowInitializer rowInitializer;

    // OpenAI 호출 직전에 호출한다. 해당 기능의 오늘 카운트가 제한 미만일 때만 원자적으로 증가시키고
    // 정상 반환하며, 제한에 도달했으면 BusinessException을 던지고 카운트는 증가하지 않는다.
    //
    // REQUIRES_NEW로 별도 트랜잭션을 사용하는 이유: 호출하는 AI 서비스 메서드가 readOnly
    // 트랜잭션(CharacterExtractionService 등)일 수도 있고, 무엇보다 "OpenAI 요청 전송 이후 실패해도
    // 이미 차감된 사용 횟수는 복구하지 않는다"는 요구사항을 만족하려면 이 증가가 호출부 트랜잭션의
    // 롤백 여부와 무관하게 즉시 커밋되어야 한다. 별도 트랜잭션으로 분리하면 이 요구사항이 자동으로
    // 보장된다(호출부가 나중에 어떤 이유로 롤백되어도 이미 커밋된 사용량 증가는 그대로 남는다).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndIncrement(Long userId, AiFeatureType featureType) {
        LocalDate today = LocalDate.now(clock);
        // 별도 빈(REQUIRES_NEW)에 위임한다. 동시에 다른 요청이 먼저 같은 행을 만들었다면 그쪽 트랜잭션의
        // 고유 제약(user_id, usage_date) 위반으로 예외가 올라오는데, 그 트랜잭션은 이미 종료(롤백)된
        // 뒤이므로 이 메서드(다른 세션)에서 안전하게 무시하고 계속 진행할 수 있다.
        try {
            rowInitializer.ensureRowExists(userId, today);
        } catch (DataIntegrityViolationException e) {
            log.debug("AI daily usage row already created concurrently. userId={}, date={}", userId, today);
        }

        int featureLimit = resolveFeatureLimit(featureType);
        int updatedRows = incrementIfAllowed(featureType, userId, today, featureLimit);
        if (updatedRows == 1) {
            return;
        }

        log.warn("AI daily feature limit exceeded. userId={}, feature={}", userId, featureType);
        throw new BusinessException(
                ErrorCode.AI_DAILY_FEATURE_LIMIT_EXCEEDED,
                "오늘 사용할 수 있는 " + featureType.getLabel() + " 횟수를 모두 사용했습니다. AI 도구 이용권은 매일 00:00에 충전됩니다."
        );
    }

    // 오늘(Asia/Seoul 기준) 기능별 사용 현황 조회 — 조회 자체는 사용 횟수에 포함되지 않는다.
    @Transactional(readOnly = true)
    public AiDailyUsageResponseDto getDailyUsage(String email) {
        User user = findUserByEmail(email);
        LocalDate today = LocalDate.now(clock);

        AiDailyUsage usage = aiDailyUsageRepository.findByUserIdAndUsageDate(user.getId(), today).orElse(null);

        return new AiDailyUsageResponseDto(
                today,
                "Asia/Seoul",
                today.plusDays(1).atStartOfDay(clock.getZone()).toOffsetDateTime(),
                detail(usage != null ? usage.getSummaryCount() : 0, limitProperties.getSummary()),
                detail(usage != null ? usage.getConflictCount() : 0, limitProperties.getConflict()),
                detail(usage != null ? usage.getCharacterCount() : 0, limitProperties.getCharacter()),
                detail(usage != null ? usage.getWorldviewCount() : 0, limitProperties.getWorldview()),
                detail(usage != null ? usage.getChatCount() : 0, limitProperties.getChat())
        );
    }

    private AiUsageDetailDto detail(int used, int featureLimit) {
        int remaining = Math.max(0, featureLimit - used);
        return new AiUsageDetailDto(used, remaining, featureLimit);
    }

    private int resolveFeatureLimit(AiFeatureType featureType) {
        return switch (featureType) {
            case EPISODE_SUMMARY -> limitProperties.getSummary();
            case CONFLICT_DETECTION -> limitProperties.getConflict();
            case CHARACTER_EXTRACTION -> limitProperties.getCharacter();
            case WORLDVIEW_EXTRACTION -> limitProperties.getWorldview();
            case AI_CHAT -> limitProperties.getChat();
        };
    }

    private int incrementIfAllowed(AiFeatureType featureType, Long userId, LocalDate today, int featureLimit) {
        return switch (featureType) {
            case EPISODE_SUMMARY -> aiDailyUsageRepository.incrementSummaryIfAllowed(userId, today, featureLimit);
            case CONFLICT_DETECTION -> aiDailyUsageRepository.incrementConflictIfAllowed(userId, today, featureLimit);
            case CHARACTER_EXTRACTION -> aiDailyUsageRepository.incrementCharacterIfAllowed(userId, today, featureLimit);
            case WORLDVIEW_EXTRACTION -> aiDailyUsageRepository.incrementWorldviewIfAllowed(userId, today, featureLimit);
            case AI_CHAT -> aiDailyUsageRepository.incrementChatIfAllowed(userId, today, featureLimit);
        };
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
