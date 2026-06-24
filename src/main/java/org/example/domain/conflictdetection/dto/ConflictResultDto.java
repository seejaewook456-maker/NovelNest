package org.example.domain.conflictdetection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConflictResultDto {

    // 충돌 유형 (CharacterCandidateDto와 동일하게 String으로 받아 Jackson이 역직렬화)
    private String type;

    // 심각도
    private String severity;

    // 충돌 제목 (예: "에테르의 서 사용 조건 충돌")
    private String title;

    // 기존 설정 내용
    private String existingInfo;

    // 현재 회차 내용
    private String currentEpisodeInfo;

    // 충돌 설명
    private String description;

    // 작가에게 전달하는 제안 사항
    private String suggestion;
}
