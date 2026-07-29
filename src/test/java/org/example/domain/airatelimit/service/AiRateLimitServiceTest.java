package org.example.domain.airatelimit.service;

import org.example.domain.airatelimit.config.AiRateLimitProperties;
import org.example.domain.airatelimit.entity.AiRequestLog;
import org.example.domain.airatelimit.repository.AiRequestLogRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 실제 DB(비관적 락, Sliding Window) 동작 검증은 AiRateLimitServiceIntegrationTest에서 다루고,
// 여기서는 리포지토리 반환값에 따른 서비스의 분기 로직만 검증한다.
@ExtendWith(MockitoExtension.class)
class AiRateLimitServiceTest {

    @Mock
    private AiRequestLogRepository aiRequestLogRepository;
    @Mock
    private UserRepository userRepository;

    private AiRateLimitProperties properties;
    private AiRateLimitService aiRateLimitService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 2026-07-29 12:00 KST 고정 — 테스트가 실제 현재 시각에 의존하지 않도록 Clock을 주입한다.
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-29T03:00:00Z"), SEOUL);
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        properties = new AiRateLimitProperties();
        properties.setMaxRequests(10);
        properties.setWindowSeconds(60);

        aiRateLimitService = new AiRateLimitService(aiRequestLogRepository, userRepository, properties, FIXED_CLOCK);

        User user = User.builder().email("writer@example.com").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
    }

    @Test
    void 최근_1분_요청이_9회면_10번째_요청은_허용되고_기록된다() {
        given(aiRequestLogRepository.countByUserIdAndRequestedAtAfter(eq(USER_ID), any())).willReturn(9L);

        aiRateLimitService.checkAndRecord(USER_ID);

        ArgumentCaptor<AiRequestLog> captor = ArgumentCaptor.forClass(AiRequestLog.class);
        verify(aiRequestLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getRequestedAt()).isEqualTo(NOW);
    }

    @Test
    void 최근_1분_요청이_10회면_11번째_요청은_전용_예외로_차단되고_기록되지_않는다() {
        given(aiRequestLogRepository.countByUserIdAndRequestedAtAfter(eq(USER_ID), any())).willReturn(10L);

        assertThatThrownBy(() -> aiRateLimitService.checkAndRecord(USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_RATE_LIMIT_EXCEEDED));

        verify(aiRequestLogRepository, never()).save(any());
    }

    @Test
    void 매_호출마다_윈도우_밖으로_나간_오래된_로그를_함께_정리한다() {
        given(aiRequestLogRepository.countByUserIdAndRequestedAtAfter(eq(USER_ID), any())).willReturn(0L);

        aiRateLimitService.checkAndRecord(USER_ID);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(aiRequestLogRepository).deleteByRequestedAtBefore(cutoffCaptor.capture());
        assertThat(cutoffCaptor.getValue()).isEqualTo(NOW.minusSeconds(60));
    }
}
