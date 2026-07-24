package org.example.global.ai.context;

import java.util.List;

// AI 채팅/설정 충돌 감지가 공통으로 사용하는 작품 참고 데이터 묶음.
// characters/worldSettings/episodeSummaries 모두 이미 개수·문자 예산 제한이 적용된 최종 결과다.
public record NovelAiContext(
        List<CharacterContext> characters,
        List<WorldSettingContext> worldSettings,
        List<EpisodeSummaryContext> episodeSummaries
) {
}
