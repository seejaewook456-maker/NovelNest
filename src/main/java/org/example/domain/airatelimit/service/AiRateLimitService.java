package org.example.domain.airatelimit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.airatelimit.config.AiRateLimitProperties;
import org.example.domain.airatelimit.entity.AiRequestLog;
import org.example.domain.airatelimit.repository.AiRequestLogRepository;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

// 사용자별 AI 전체 요청에 대한 분당 Rate Limit(최근 60초 Sliding Window) 검사.
// 5개 AI 기능(요약/충돌감지/인물추출/세계관추출/챗봇)이 기능 구분 없이 이 클래스 하나를 공통으로
// 거치며, "AI 전체 요청" 합계 기준으로 최근 windowSeconds초 동안 maxRequests회까지만 허용한다.
// 각 AI 기능 서비스에서 AiUsageService.checkAndIncrement(하루 사용량 검사)보다 반드시 먼저 호출해야
// 한다 — 분당 제한에 걸리면 하루 사용량도 차감되지 않고 OpenAI도 호출되지 않아야 하기 때문이다.
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimitService {

    private final AiRequestLogRepository aiRequestLogRepository;
    private final UserRepository userRepository;
    private final AiRateLimitProperties properties;
    private final Clock clock;

    // Redis 없이 DB만으로 "최근 60초 요청 수 조회 → 미만이면 기록"을 원자적으로 처리해야 한다.
    // AiUsageService처럼 조건부 UPDATE 한 방으로 끝낼 수 없는 이유는, 이건 고정 카운터 증가가 아니라
    // COUNT(*) 기반 판단(Sliding Window)이라 "조회"와 "기록(INSERT)"이 분리된 두 단계일 수밖에 없기
    // 때문이다. 그래서 users 테이블의 해당 사용자 행에 비관적 잠금(SELECT ... FOR UPDATE)을 걸어
    // 조회~기록 구간을 직렬화한다 — 사용자가 다르면 잠그는 행도 달라 서로 막지 않는다.
    //
    // REQUIRES_NEW로 별도 트랜잭션을 쓰는 이유: 잠금을 오래 들고 있으면 같은 사용자의 동시 요청이
    // OpenAI 응답 대기 시간만큼 줄줄이 대기하게 된다. 별도 트랜잭션으로 분리해 이 메서드가 끝나자마자
    // (=OpenAI 호출 전에) 즉시 커밋되어 잠금이 곧바로 풀리도록 한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkAndRecord(Long userId) {
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime windowStart = now.minusSeconds(properties.getWindowSeconds());

        long recentCount = aiRequestLogRepository.countByUserIdAndRequestedAtAfter(userId, windowStart);
        if (recentCount >= properties.getMaxRequests()) {
            log.warn("AI rate limit exceeded. userId={}, recentCount={}, windowSeconds={}",
                    userId, recentCount, properties.getWindowSeconds());
            throw new BusinessException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }

        aiRequestLogRepository.save(new AiRequestLog(userId, now));

        // 배치/스케줄러 없이, 매 요청마다 이번 사용자와 무관하게 윈도우 밖으로 나간 전체 오래된 로그를
        // 함께 정리해 테이블이 무한히 커지지 않게 한다(활성 사용자 수 x 분당 최대 요청 수 규모로 유지).
        aiRequestLogRepository.deleteByRequestedAtBefore(windowStart);
    }
}
