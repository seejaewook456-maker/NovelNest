package org.example.domain.aiusage.service;

import org.example.domain.aiusage.config.AiUsageLimitProperties;
import org.example.domain.aiusage.dto.AiDailyUsageResponseDto;
import org.example.domain.aiusage.entity.AiDailyUsage;
import org.example.domain.aiusage.enums.AiFeatureType;
import org.example.domain.aiusage.repository.AiDailyUsageRepository;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 실제 DB 원자성(동시성) 검증은 AiUsageServiceIntegrationTest에서 다루고,
// 여기서는 리포지토리 반환값에 따른 서비스의 분기 로직(어떤 예외를 던지는지, remaining 계산 등)을 검증한다.
// 전체 합계 제한은 없고 기능별 제한만 존재한다.
@ExtendWith(MockitoExtension.class)
class AiUsageServiceTest {

    @Mock
    private AiDailyUsageRepository aiDailyUsageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AiDailyUsageRowInitializer rowInitializer;

    private AiUsageLimitProperties limitProperties;
    private AiUsageService aiUsageService;

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 2026-07-29 12:00 KST 고정 — 테스트가 실제 현재 시각에 의존하지 않도록 Clock을 주입한다.
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-07-29T03:00:00Z"), SEOUL);
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 29);
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        limitProperties = new AiUsageLimitProperties();
        limitProperties.setSummary(20);
        limitProperties.setConflict(15);
        limitProperties.setCharacter(15);
        limitProperties.setWorldview(15);
        limitProperties.setChat(20);

        aiUsageService = new AiUsageService(aiDailyUsageRepository, limitProperties, userRepository, FIXED_CLOCK, rowInitializer);
    }

    @Test
    void 오늘_사용량_행_생성은_전용_초기화_컴포넌트에_위임한다() {
        given(aiDailyUsageRepository.incrementSummaryIfAllowed(eq(USER_ID), eq(TODAY), eq(20))).willReturn(1);

        aiUsageService.checkAndIncrement(USER_ID, AiFeatureType.EPISODE_SUMMARY);

        // 행 생성/중복키 처리는 AiDailyUsageRowInitializer 자체 테스트에서 검증하고,
        // 여기서는 checkAndIncrement가 그 위임을 실제로 호출하는지만 확인한다.
        verify(rowInitializer).ensureRowExists(USER_ID, TODAY);
    }

    @Test
    void 기능별_제한_초과_시_전용_예외와_기능명이_포함된_메시지를_던진다() {
        given(aiDailyUsageRepository.incrementSummaryIfAllowed(USER_ID, TODAY, 20)).willReturn(0);

        assertThatThrownBy(() -> aiUsageService.checkAndIncrement(USER_ID, AiFeatureType.EPISODE_SUMMARY))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AI_DAILY_FEATURE_LIMIT_EXCEEDED))
                .hasMessageContaining("회차 요약");
    }

    @Test
    void 제한_이내면_예외_없이_증가만_하고_반환한다() {
        given(aiDailyUsageRepository.incrementChatIfAllowed(USER_ID, TODAY, 20)).willReturn(1);

        aiUsageService.checkAndIncrement(USER_ID, AiFeatureType.AI_CHAT);

        verify(aiDailyUsageRepository).incrementChatIfAllowed(USER_ID, TODAY, 20);
    }

    @Test
    void 사용량_조회는_증가_메서드를_전혀_호출하지_않는다() {
        User user = User.builder().email("writer@example.com").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findByEmailAndDeletedAtIsNull("writer@example.com")).willReturn(Optional.of(user));
        given(aiDailyUsageRepository.findByUserIdAndUsageDate(USER_ID, TODAY)).willReturn(Optional.empty());

        aiUsageService.getDailyUsage("writer@example.com");

        verify(aiDailyUsageRepository, never()).incrementSummaryIfAllowed(any(), any(), anyInt());
        verify(aiDailyUsageRepository, never()).incrementConflictIfAllowed(any(), any(), anyInt());
        verify(aiDailyUsageRepository, never()).incrementCharacterIfAllowed(any(), any(), anyInt());
        verify(aiDailyUsageRepository, never()).incrementWorldviewIfAllowed(any(), any(), anyInt());
        verify(aiDailyUsageRepository, never()).incrementChatIfAllowed(any(), any(), anyInt());
        verify(rowInitializer, never()).ensureRowExists(any(), any());
    }

    @Test
    void 사용량이_전혀_없으면_조회_응답은_전부_0사용_한도_그대로다() {
        User user = User.builder().email("writer@example.com").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findByEmailAndDeletedAtIsNull("writer@example.com")).willReturn(Optional.of(user));
        given(aiDailyUsageRepository.findByUserIdAndUsageDate(USER_ID, TODAY)).willReturn(Optional.empty());

        AiDailyUsageResponseDto response = aiUsageService.getDailyUsage("writer@example.com");

        assertThat(response.getUsageDate()).isEqualTo(TODAY);
        assertThat(response.getTimezone()).isEqualTo("Asia/Seoul");
        assertThat(response.getSummary().getUsed()).isZero();
        assertThat(response.getSummary().getRemaining()).isEqualTo(20);
        assertThat(response.getChat().getRemaining()).isEqualTo(20);
    }

    @Test
    void remaining은_기능_자체의_잔여량이다() {
        User user = User.builder().email("writer@example.com").nickname("작가").provider(Provider.LOCAL).build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        given(userRepository.findByEmailAndDeletedAtIsNull("writer@example.com")).willReturn(Optional.of(user));
        // summary 10회 사용 → 20 - 10 = 10 남음
        given(aiDailyUsageRepository.findByUserIdAndUsageDate(USER_ID, TODAY))
                .willReturn(Optional.of(usageOf(10, 0, 0, 0, 0)));

        AiDailyUsageResponseDto response = aiUsageService.getDailyUsage("writer@example.com");

        assertThat(response.getSummary().getUsed()).isEqualTo(10);
        assertThat(response.getSummary().getRemaining()).isEqualTo(10);
        assertThat(response.getSummary().getLimit()).isEqualTo(20);
    }

    private AiDailyUsage usageOf(int summary, int conflict, int character, int worldview, int chat) {
        AiDailyUsage usage = AiDailyUsage.builder().userId(USER_ID).usageDate(TODAY).build();
        for (int i = 0; i < summary; i++) increment(usage, AiFeatureType.EPISODE_SUMMARY);
        for (int i = 0; i < conflict; i++) increment(usage, AiFeatureType.CONFLICT_DETECTION);
        for (int i = 0; i < character; i++) increment(usage, AiFeatureType.CHARACTER_EXTRACTION);
        for (int i = 0; i < worldview; i++) increment(usage, AiFeatureType.WORLDVIEW_EXTRACTION);
        for (int i = 0; i < chat; i++) increment(usage, AiFeatureType.AI_CHAT);
        return usage;
    }

    // 테스트 전용 헬퍼 — 엔티티에 프로덕션 코드에서 쓰지 않는 증가 메서드를 추가하지 않기 위해 리플렉션으로 카운트를 직접 채운다.
    private void increment(AiDailyUsage usage, AiFeatureType type) {
        switch (type) {
            case EPISODE_SUMMARY -> ReflectionTestUtils.setField(usage, "summaryCount", usage.getSummaryCount() + 1);
            case CONFLICT_DETECTION -> ReflectionTestUtils.setField(usage, "conflictCount", usage.getConflictCount() + 1);
            case CHARACTER_EXTRACTION -> ReflectionTestUtils.setField(usage, "characterCount", usage.getCharacterCount() + 1);
            case WORLDVIEW_EXTRACTION -> ReflectionTestUtils.setField(usage, "worldviewCount", usage.getWorldviewCount() + 1);
            case AI_CHAT -> ReflectionTestUtils.setField(usage, "chatCount", usage.getChatCount() + 1);
        }
    }
}
