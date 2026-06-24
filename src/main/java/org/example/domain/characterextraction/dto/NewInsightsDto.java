package org.example.domain.characterextraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// AI가 기존 인물에서 새롭게 발견한 정보 — 신규 인물이면 null
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewInsightsDto {

    private List<String> personality;
    private List<String> speechStyle;
}
