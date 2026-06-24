package org.example.domain.characterextraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.domain.character.dto.CharacterResponseDto;

// OpenAI 응답 JSON 역직렬화 + 클라이언트 응답 DTO를 겸한다.
// isExistingCharacter: OpenAI가 설정하는 기존/신규 구분 플래그
// existingCharacter: 서비스가 DB 조회 후 직접 보강하는 기존 인물 상세 정보
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CharacterCandidateDto {

    private String name;
    private String role;
    private Integer age;
    private String personality;
    private String speechStyle;
    private String description;

    // 해당 특성을 뒷받침하는 회차 내 근거 장면 또는 대사
    private String evidence;

    // OpenAI가 기존 인물 여부를 설정 — @JsonProperty로 JSON 키 "isExistingCharacter" 고정
    @JsonProperty("isExistingCharacter")
    private Boolean isExistingCharacter;

    // 기존 인물일 때 매칭된 Character ID — OpenAI가 설정
    private Long matchedCharacterId;

    // 기존 인물의 기존 정보와 비교해 새롭게 발견된 정보 — OpenAI가 설정
    private NewInsightsDto newInsights;

    // 기존 인물의 DB 전체 정보 — 서비스가 matchedCharacterId로 조회 후 설정
    private CharacterResponseDto existingCharacter;
}
