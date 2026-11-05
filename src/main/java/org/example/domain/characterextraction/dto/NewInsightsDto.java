package org.example.domain.characterextraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// AI가 기존 인물에서 새롭게 발견한 정보 — 신규 인물이면 null
// 기존 필드(personality, speechStyle)는 그대로 두고, 보완/수정 판단을 위해 필드를 추가했다.
// (추가 필드는 값이 없으면 null/빈 배열이므로 기존 프론트와도 호환된다)
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NewInsightsDto {

    private List<String> personality;
    private List<String> speechStyle;

    // 외모·능력·배경·관계 등 설명(description)에 보완할 새 정보
    private List<String> description;

    // 이번 회차에서 기존과 "다르게" 확인된 역할 — 변경이 없으면 null
    private String role;

    // 이번 회차에서 기존과 "다르게" 확인된 나이 — 변경이 없으면 null
    private Integer age;
}
