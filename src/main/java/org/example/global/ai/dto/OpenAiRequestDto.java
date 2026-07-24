package org.example.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

// OpenAI Responses API (POST /v1/responses) 요청 바디
@Getter
@Builder
public class OpenAiRequestDto {

    @JsonProperty("model")
    private String model;

    // AI 역할 지시문 (예: "당신은 소설 편집 전문가입니다.")
    @JsonProperty("instructions")
    private String instructions;

    // 실제 처리할 텍스트 (예: 에피소드 본문)
    @JsonProperty("input")
    private String input;

    // 응답 최대 출력 토큰 — null이면 요청 본문에서 생략되어 기존 호출부는 동작이 바뀌지 않는다.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("max_output_tokens")
    private Integer maxOutputTokens;
}
