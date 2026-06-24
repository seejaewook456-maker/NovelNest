package org.example.domain.characterextraction.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class CharacterExtractionResponseDto {

    // "1화 - 제목" 형식 — 어느 회차 분석 결과인지 명시
    private final String episodeTitle;

    // 추출된 후보 총 수 — 프론트 "1/5" 표시에 사용
    private final int totalCount;

    private final List<CharacterCandidateDto> candidates;
}
