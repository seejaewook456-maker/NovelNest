package org.example.domain.worldsettingextraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.domain.worldsetting.entity.WorldSettingCategory;

// OpenAI 응답 JSON 역직렬화 + 클라이언트 응답 DTO를 겸한다.
// isExistingSetting: OpenAI가 설정하는 기존/신규 구분 플래그
// existingWorldSetting: 서비스가 DB 조회 후 직접 보강하는 기존 설정 상세 정보
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorldSettingCandidateDto {

    // COUNTRY / RACE / MAGIC / ORGANIZATION / PLACE / EVENT / ITEM / RULE / ETC
    // AI가 프롬프트 규칙을 벗어난 값을 반환해도 예외 대신 ETC로 대체 (WorldSettingCategoryDeserializer)
    @JsonDeserialize(using = WorldSettingCategoryDeserializer.class)
    private WorldSettingCategory category;

    private String title;

    // AI가 제안하는 전체 content (신규 or 보강된 기존 content)
    private String content;

    // 해당 설정을 뒷받침하는 회차 내 근거 장면 또는 대사 — 수정 불가
    private String evidence;

    // OpenAI가 기존 설정 여부를 설정 — @JsonProperty로 JSON 키 고정
    @JsonProperty("isExistingSetting")
    private Boolean isExistingSetting;

    // 기존 설정일 때 매칭된 WorldSetting ID — OpenAI가 설정
    private Long matchedWorldSettingId;

    // 기존 설정의 DB 전체 정보 — 서비스가 matchedWorldSettingId로 조회 후 보강
    private WorldSettingResponseDto existingWorldSetting;

    // 기존 설정 대비 새롭게 발견된 정보 — OpenAI가 설정, 기존 설정에만 존재
    private WorldSettingNewInsightsDto newInsights;

    // 기존 설정 보강 시 이번 회차에서 새로 추가되는 문단 내용만 담은 필드 — OpenAI가 설정, 기존 설정에만 존재.
    // 서비스가 이 값을 기존 WorldSetting.content 뒤에 "\n\n"으로 이어붙여 content를 다시 계산하므로,
    // 기존 내용이 AI에 의해 재작성/요약되지 않고 그대로 보존된다.
    private String newContent;
}
