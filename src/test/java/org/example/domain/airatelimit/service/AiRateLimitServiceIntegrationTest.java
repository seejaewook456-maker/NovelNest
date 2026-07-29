package org.example.domain.airatelimit.service;

import org.example.domain.airatelimit.config.AiRateLimitProperties;
import org.example.domain.airatelimit.repository.AiRequestLogRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.config.ClockConfig;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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

// 실제 DB(H2, ai_request_logs 테이블)로 Sliding Window 카운트와 비관적 락 기반 동시성 제어를 검증한다.
// @DataJpaTest는 클래스패스의 H2를 자동으로 인메모리 DB로 구성한다(src/test/resources/application.yml
// 에서 Flyway는 꺼두고 Hibernate가 엔티티 기준으로 스키마를 즉석 생성하도록 했다 — AiUsageServiceIntegrationTest와 동일).
@DataJpaTest
@Import({AiRateLimitService.class, AiRateLimitProperties.class, ClockConfig.class})
class AiRateLimitServiceIntegrationTest {

    @Autowired
    private AiRequestLogRepository aiRequestLogRepository;
    @Autowired
    private UserRepository userRepository;
    // 동시성 테스트 전용 — 실제 Spring 프록시가 적용된 빈이라 @Transactional(REQUIRES_NEW)이 정말로 동작한다.
    @Autowired
    private AiRateLimitService managedAiRateLimitService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private AiRateLimitService serviceAt(Instant instant, AiRateLimitProperties properties) {
        return new AiRateLimitService(aiRequestLogRepository, userRepository, properties, Clock.fixed(instant, SEOUL));
    }

    private AiRateLimitProperties defaultProperties() {
        AiRateLimitProperties properties = new AiRateLimitProperties();
        properties.setMaxRequests(10);
        properties.setWindowSeconds(60);
        return properties;
    }

    private Long persistUser(String email) {
        User user = User.builder().email(email).nickname("작가").provider(Provider.LOCAL).build();
        return userRepository.save(user).getId();
    }

    // 전역 count() 대신 사용자별로 스코프를 좁혀 검증한다 — 동시성 테스트가 커밋한 데이터가
    // 같은 클래스 내 다른 테스트의 count()에 섞여 들어가는 것을 원천적으로 피하기 위함이다.
    private long countAllForUser(Long userId) {
        return aiRequestLogRepository.countByUserIdAndRequestedAtAfter(userId, LocalDateTime.of(2000, 1, 1, 0, 0));
    }

    @Test
    void 최근_60초_요청이_9회면_10번째_요청은_성공한다() {
        Long userId = persistUser("writer1@example.com");
        Instant base = Instant.parse("2026-07-29T03:30:00Z");
        AiRateLimitService service = serviceAt(base, defaultProperties());

        for (int i = 1; i <= 9; i++) {
            service.checkAndRecord(userId);
        }

        service.checkAndRecord(userId); // 10번째 — 예외 없이 성공해야 한다
        assertThat(countAllForUser(userId)).isEqualTo(10);
    }

    @Test
    void 최근_60초_요청이_10회면_11번째_요청은_429로_차단된다() {
        Long userId = persistUser("writer2@example.com");
        Instant base = Instant.parse("2026-07-29T03:30:00Z");
        AiRateLimitService service = serviceAt(base, defaultProperties());

        for (int i = 1; i <= 10; i++) {
            service.checkAndRecord(userId);
        }

        assertThatThrownBy(() -> service.checkAndRecord(userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_RATE_LIMIT_EXCEEDED));

        // 차단된 요청은 기록되지 않으므로 로그는 여전히 10건이어야 한다.
        assertThat(countAllForUser(userId)).isEqualTo(10);
    }

    @Test
    void 윈도우_경과_61초_후_오래된_요청이_밖으로_나가면_다시_요청_가능하다() {
        Long userId = persistUser("writer3@example.com");
        Instant base = Instant.parse("2026-07-29T03:30:00Z");
        AiRateLimitProperties properties = defaultProperties();
        AiRateLimitService serviceAtBase = serviceAt(base, properties);

        for (int i = 1; i <= 10; i++) {
            serviceAtBase.checkAndRecord(userId);
        }
        assertThatThrownBy(() -> serviceAtBase.checkAndRecord(userId)).isInstanceOf(BusinessException.class);

        // 61초 뒤 — 10건 모두 Sliding Window(최근 60초) 밖으로 나가므로 다시 허용되어야 한다.
        AiRateLimitService serviceAfter61s = serviceAt(base.plusSeconds(61), properties);
        serviceAfter61s.checkAndRecord(userId);

        // 오래된 10건은 이번 호출에서 함께 정리되고, 새 기록 1건만 남아야 한다.
        assertThat(countAllForUser(userId)).isEqualTo(1);
    }

    // 요구사항 6: 챗봇/요약/세계관 등 어떤 기능에서 호출하든 checkAndRecord는 기능을 구분하지 않고
    // "AI 전체 요청" 합계 하나로만 판단한다 — 그래서 서로 다른 기능에서 왔다고 가정한 호출을 섞어도
    // 합계 10회를 넘는 순간 차단되는지 확인한다.
    @Test
    void 여러_AI_기능을_섞어서_호출해도_전체_요청_합계_기준으로_제한된다() {
        Long userId = persistUser("writer4@example.com");
        Instant base = Instant.parse("2026-07-29T03:30:00Z");
        AiRateLimitService service = serviceAt(base, defaultProperties());

        for (int i = 0; i < 5; i++) service.checkAndRecord(userId); // 챗봇 5회(가정)
        for (int i = 0; i < 3; i++) service.checkAndRecord(userId); // 회차 요약 3회(가정)
        for (int i = 0; i < 2; i++) service.checkAndRecord(userId); // 세계관 추출 2회(가정) — 합계 10회

        assertThatThrownBy(() -> service.checkAndRecord(userId)) // 11번째(등장인물 추출이라고 가정) — 차단
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_RATE_LIMIT_EXCEEDED));
    }

    @Test
    void 다른_사용자의_요청은_서로_섞이지_않는다() {
        AiRateLimitService service = serviceAt(Instant.parse("2026-07-29T03:30:00Z"), defaultProperties());
        Long userA = persistUser("writerA@example.com");
        Long userB = persistUser("writerB@example.com");

        for (int i = 0; i < 10; i++) service.checkAndRecord(userA);
        assertThatThrownBy(() -> service.checkAndRecord(userA)).isInstanceOf(BusinessException.class);

        // userA가 이미 한도를 채웠어도 userB는 영향받지 않아야 한다.
        service.checkAndRecord(userB);
    }

    // 동시 요청에서도 분당 제한을 우회할 수 없는지 — 같은 사용자에 대해 한도(10)보다 많은(30) 스레드가
    // 동시에 checkAndRecord를 호출해도 성공은 정확히 한도만큼만 일어나야 한다. 실제 스레드 풀에서
    // 실행되므로 Spring이 관리하는(=@Transactional이 실제로 적용되는) managedAiRateLimitService를 사용한다.
    @Test
    void 동시_요청에서도_분당_제한을_우회할_수_없다() throws InterruptedException {
        Long userId = persistUser("writer-concurrent@example.com");
        // managedAiRateLimitService는 REQUIRES_NEW로 스레드마다 별도 커넥션/트랜잭션을 연다 — 방금
        // persistUser()로 저장한 User가 아직 이 테스트 메서드의 트랜잭션 안에 있어 다른 커넥션에서는
        // 보이지 않는다(격리 수준상 미커밋 데이터는 조회 불가). 스레드를 띄우기 전에 강제로 커밋해
        // 실제 운영에서처럼(이미 존재하는 회원) 다른 트랜잭션에서도 조회 가능하게 만든다.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        int limit = 10; // AiRateLimitProperties 기본값(maxRequests)
        int concurrentRequests = 30;

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
                        managedAiRateLimitService.checkAndRecord(userId);
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
        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        if (!unexpected.isEmpty()) {
            unexpected.forEach(Throwable::printStackTrace);
        }
        assertThat(unexpected).isEmpty();
        assertThat(successCount.get()).isEqualTo(limit);
        assertThat(blockedCount.get()).isEqualTo(concurrentRequests - limit);
        assertThat(countAllForUser(userId)).isEqualTo(limit);
    }
}
