package org.example.domain.aiusage.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// GET /api/ai/usage/daily 응답 — 오늘(Asia/Seoul 기준) AI 기능별 사용 현황.
@Getter
@RequiredArgsConstructor
public class AiDailyUsageResponseDto {

    private final LocalDate usageDate;
    private final String timezone;
    private final OffsetDateTime nextResetAt;
    private final AiUsageDetailDto summary;
    private final AiUsageDetailDto conflict;
    private final AiUsageDetailDto character;
    private final AiUsageDetailDto worldview;
    private final AiUsageDetailDto chat;
}
