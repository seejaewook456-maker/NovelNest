package org.example.global.ai.context;

// AI 프롬프트에 전달할 회차 요약 정보 — EpisodeSummary/Episode 엔티티를 직접 노출하지 않기 위한 DTO.
// summaries 리스트는 항상 회차 번호 오름차순(과거 → 최신)으로 정렬되어 내려온다.
public record EpisodeSummaryContext(
        int episodeNumber,
        String episodeTitle,
        String summary
) {
}
