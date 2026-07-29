package org.example.domain.aiusage.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 기능 하나(또는 전체) 기준의 오늘 사용 현황.
// remaining은 해당 기능 자체의 남은 횟수와 전체 남은 횟수 중 더 작은 값 — 즉 "실제로 추가 실행 가능한 횟수"다.
@Getter
@RequiredArgsConstructor
public class AiUsageDetailDto {

    private final int used;
    private final int remaining;
    private final int limit;
}
