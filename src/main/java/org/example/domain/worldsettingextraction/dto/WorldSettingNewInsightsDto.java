package org.example.domain.worldsettingextraction.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 기존 세계관 설정에 대해 AI가 새롭게 발견한 정보 목록
// OpenAI가 간혹 {"content":[...]} 대신 배열([...]) 형태로 그냥 반환하는 경우가 있어
// WorldSettingNewInsightsDeserializer로 두 형태를 모두 허용해서 파싱한다.
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonDeserialize(using = WorldSettingNewInsightsDeserializer.class)
public class WorldSettingNewInsightsDto {

    // 기존 content에 없는 새로운 설정 정보 항목들
    private List<String> content;
}
