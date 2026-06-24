package org.example.global.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

// OpenAI Responses API 응답 파싱용 DTO
// 구조: output[0].content[0].text 에 AI 생성 텍스트가 담긴다
@Getter
public class OpenAiResponseDto {

    @JsonProperty("output")
    private List<Output> output;

    // AI 응답에서 텍스트를 꺼내는 헬퍼 메서드
    public String getFirstText() {
        if (output == null || output.isEmpty()) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다.");
        }
        List<Content> contentList = output.get(0).getContent();
        if (contentList == null || contentList.isEmpty()) {
            throw new IllegalArgumentException("AI 응답 내용이 비어 있습니다.");
        }
        return contentList.get(0).getText();
    }

    @Getter
    public static class Output {
        @JsonProperty("content")
        private List<Content> content;
    }

    @Getter
    public static class Content {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;
    }
}
