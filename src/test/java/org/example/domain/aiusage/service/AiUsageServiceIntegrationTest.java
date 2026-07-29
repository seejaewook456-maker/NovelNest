package org.example.domain.aiusage.service;

import org.example.domain.aiusage.config.AiUsageLimitProperties;
import org.example.domain.aiusage.entity.AiDailyUsage;
import org.example.domain.aiusage.enums.AiFeatureType;
import org.example.domain.aiusage.repository.AiDailyUsageRepository;
import org.example.domain.user.repository.UserRepository;
import org.example.global.config.ClockConfig;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 실제 DB(H2, ai_daily_usages 테이블)를 사용해 원자적 조건부 UPDATE의 실제 동작을 검증한다.
// @DataJpaTest는 클래스패스의 H2를 자동으로 인메모리 DB로 구성해준다(src/test/resources/application.yml
// 에서 Flyway는 꺼두고 Hibernate가 엔티티 기준으로 스키마를 즉석 생성하도록 했다).
//
// 대부분의 테스트는 serviceAt(...)으로 AiUsageService를 직접 new해서 쓴다 — @DataJpaTest는 테스트
// 메서드 전체를 하나의 트랜잭션으로 감싸므로(끝나면 롤백), 같은 스레드에서 순차 호출하는 한
// @Modifying 쿼리도 그 트랜잭션에 자연스럽게 편승해 정상 동작하고, 조건부 UPDATE의 실제 SQL 동작을
// 그대로 검증할 수 있다. 다만 이 방식은 AiUsageService가 Spring 빈이 아니므로 @Transactional이
// 실제로 적용되지 않는다는 뜻이기도 하다(단일 스레드에서는 결과에 차이가 없다).
// 반면 동시성 테스트는 별도 스레드에서 호출하므로 테스트 메서드의 트랜잭션에 편승할 수 없어
// TransactionRequiredException이 발생한다 — 그래서 그 테스트만 @Import로 실제 Spring이 관리하는
// (프록시가 적용되어 @Transactional(REQUIRES_NEW)가 진짜로 동작하는) 빈을 사용한다.
@DataJpaTest
@Import({AiUsageService.class, AiDailyUsageRowInitializer.class, AiUsageLimitProperties.class, ClockConfig.class})
class AiUsageServiceIntegrationTest {

    @Autowired
    private AiDailyUsageRepository aiDailyUsageRepository;
    @Autowired
    private UserRepository userRepository;
    // 동시성 테스트 전용 — 실제 Spring 프록시가 적용된 빈이라 @Transactional(REQUIRES_NEW)이 정말로 동작한다.
    @Autowired
    private AiUsageService managedAiUsageService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private AiUsageService serviceAt(LocalDate date, AiUsageLimitProperties limits) {
        Clock clock = Clock.fixed(date.atStartOfDay(SEOUL).plusHours(12).toInstant(), SEOUL);
        return new AiUsageService(aiDailyUsageRepository, limits, userRepository, clock,
                new AiDailyUsageRowInitializer(aiDailyUsageRepository));
    }

    private AiUsageLimitProperties defaultLimits() {
        AiUsageLimitProperties limits = new AiUsageLimitProperties();
        limits.setSummary(20);
        limits.setConflict(15);
        limits.setCharacter(15);
        limits.setWorldview(15);
        limits.setChat(20);
        return limits;
    }

    @Test
    void 회차_요약을_20회_사용하면_21번째_요청은_차단된다() {
        Long userId = 1001L;
        AiUsageService service = serviceAt(LocalDate.of(2026, 7, 29), defaultLimits());

        for (int i = 1; i <= 20; i++) {
            service.checkAndIncrement(userId, AiFeatureType.EPISODE_SUMMARY);
        }

        assertThatThrownBy(() -> service.checkAndIncrement(userId, AiFeatureType.EPISODE_SUMMARY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_DAILY_FEATURE_LIMIT_EXCEEDED));

        AiDailyUsage usage = aiDailyUsageRepository
                .findByUserIdAndUsageDate(userId, LocalDate.of(2026, 7, 29)).orElseThrow();
        assertThat(usage.getSummaryCount()).isEqualTo(20);
    }

    @Test
    void 설정_충돌_감지를_15회_사용하면_16번째_요청은_차단된다() {
        Long userId = 1002L;
        AiUsageService service = serviceAt(LocalDate.of(2026, 7, 29), defaultLimits());

        for (int i = 1; i <= 15; i++) {
            service.checkAndIncrement(userId, AiFeatureType.CONFLICT_DETECTION);
        }

        assertThatThrownBy(() -> service.checkAndIncrement(userId, AiFeatureType.CONFLICT_DETECTION))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_DAILY_FEATURE_LIMIT_EXCEEDED));
    }

    @Test
    void 등장인물_추출을_15회_사용하면_16번째_요청은_차단된다() {
        Long userId = 1003L;
        AiUsageService service = serviceAt(LocalDate.of(2026, 7, 29), defaultLimits());

        for (int i = 1; i <= 15; i++) {
            service.checkAndIncrement(userId, AiFeatureType.CHARACTER_EXTRACTION);
        }

        assertThatThrownBy(() -> service.checkAndIncrement(userId, AiFeatureType.CHARACTER_EXTRACTION))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 세계관_추출을_15회_사용하면_16번째_요청은_차단된다() {
        Long userId = 1004L;
        AiUsageService service = serviceAt(LocalDate.of(2026, 7, 29), defaultLimits());

        for (int i = 1; i <= 15; i++) {
            service.checkAndIncrement(userId, AiFeatureType.WORLDVIEW_EXTRACTION);
        }

        assertThatThrownBy(() -> service.checkAndIncrement(userId, AiFeatureType.WORLDVIEW_EXTRACTION))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void AI_챗봇을_20회_사용하면_21번째_요청은_차단된다() {
        Long userId = 1005L;
        AiUsageService service = serviceAt(LocalDate.of(2026, 7, 29), defaultLimits());

        for (int i = 1; i <= 20; i++) {
            service.checkAndIncrement(userId, AiFeatureType.AI_CHAT);
        }

        assertThatThrownBy(() -> service.checkAndIncrement(userId, AiFeatureType.AI_CHAT))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_DAILY_FEATURE_LIMIT_EXCEEDED));
    }

    @Test
    void Asia_Seoul_기준_날짜가_바뀌면_새_날짜로_다시_사용할_수_있다() {
        Long userId = 1007L;
        AiUsageLimitProperties limits = defaultLimits();
        limits.setSummary(1); // 하루 1회로 좁혀 경계를 쉽게 확인

        AiUsageService day1Service = serviceAt(LocalDate.of(2026, 7, 29), limits);
        day1Service.checkAndIncrement(userId, AiFeatureType.EPISODE_SUMMARY);
        assertThatThrownBy(() -> day1Service.checkAndIncrement(userId, AiFeatureType.EPISODE_SUMMARY))
                .isInstanceOf(BusinessException.class);

        // 다음날(Asia/Seoul 기준)로 넘어간 새 Clock으로 같은 사용자가 다시 호출 — 새 행이 생성되어 허용되어야 한다.
        AiUsageService day2Service = serviceAt(LocalDate.of(2026, 7, 30), limits);
        day2Service.checkAndIncrement(userId, AiFeatureType.EPISODE_SUMMARY);

        AiDailyUsage day1Usage = aiDailyUsageRepository.findByUserIdAndUsageDate(userId, LocalDate.of(2026, 7, 29)).orElseThrow();
        AiDailyUsage day2Usage = aiDailyUsageRepository.findByUserIdAndUsageDate(userId, LocalDate.of(2026, 7, 30)).orElseThrow();
        assertThat(day1Usage.getSummaryCount()).isEqualTo(1);
        assertThat(day2Usage.getSummaryCount()).isEqualTo(1);
    }

    @Test
    void 다른_사용자의_사용량은_서로_섞이지_않는다() {
        AiUsageService service = serviceAt(LocalDate.of(2026, 7, 29), defaultLimits());
        Long userA = 1008L;
        Long userB = 1009L;

        service.checkAndIncrement(userA, AiFeatureType.EPISODE_SUMMARY);
        service.checkAndIncrement(userA, AiFeatureType.EPISODE_SUMMARY);
        service.checkAndIncrement(userB, AiFeatureType.EPISODE_SUMMARY);

        AiDailyUsage usageA = aiDailyUsageRepository.findByUserIdAndUsageDate(userA, LocalDate.of(2026, 7, 29)).orElseThrow();
        AiDailyUsage usageB = aiDailyUsageRepository.findByUserIdAndUsageDate(userB, LocalDate.of(2026, 7, 29)).orElseThrow();
        assertThat(usageA.getSummaryCount()).isEqualTo(2);
        assertThat(usageB.getSummaryCount()).isEqualTo(1);
    }

    // 동시 요청에서도 제한을 초과하지 않는지 — 같은 사용자에 대해 한도(AiUsageLimitProperties 기본값인
    // summary=20)보다 많은(50) 스레드가 동시에 checkAndIncrement를 호출해도, 성공은 정확히 한도만큼만
    // 일어나야 한다. 실제 스레드 풀에서 실행되므로 Spring이 관리하는(=@Transactional이 실제로 적용되는)
    // managedAiUsageService를 사용한다.
    @Test
    void 동시_요청에서도_제한_횟수를_초과하지_않는다() throws InterruptedException {
        Long userId = 1010L;
        int limit = 20; // AiUsageLimitProperties 기본값(summary)
        int concurrentRequests = 50;

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        CountDownLatch readyLatch = new CountDownLatch(concurrentRequests);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger blockedCount = new AtomicInteger();

        List<Throwable> unexpected = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        List<Runnable> tasks = IntStream.range(0, concurrentRequests)
                .mapToObj(i -> (Runnable) () -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        managedAiUsageService.checkAndIncrement(userId, AiFeatureType.EPISODE_SUMMARY);
                        successCount.incrementAndGet();
                    } catch (BusinessException e) {
                        blockedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable t) {
                        unexpected.add(t);
                    }
                })
                .collect(Collectors.toList());

        tasks.forEach(executor::submit);
        readyLatch.await(5, TimeUnit.SECONDS); // 모든 스레드가 동시에 출발할 수 있도록 대기
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        if (!unexpected.isEmpty()) {
            unexpected.forEach(Throwable::printStackTrace);
        }
        assertThat(unexpected).isEmpty();
        assertThat(successCount.get()).isEqualTo(limit);
        assertThat(blockedCount.get()).isEqualTo(concurrentRequests - limit);

        // managedAiUsageService는 실제 시스템 Clock(Asia/Seoul)을 쓰므로 오늘 날짜로 조회한다.
        LocalDate today = LocalDate.now(SEOUL);
        AiDailyUsage usage = aiDailyUsageRepository.findByUserIdAndUsageDate(userId, today).orElseThrow();
        assertThat(usage.getSummaryCount()).isEqualTo(limit);
    }
}
