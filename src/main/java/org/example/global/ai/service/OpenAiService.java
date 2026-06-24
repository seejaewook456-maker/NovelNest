package org.example.global.ai.service;

import lombok.RequiredArgsConstructor;
import org.example.global.ai.config.OpenAiConfig;
import org.example.global.ai.dto.OpenAiRequestDto;
import org.example.global.ai.dto.OpenAiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class OpenAiService {

    private final RestClient openAiRestClient;
    private final OpenAiConfig openAiConfig;

    /**
     * OpenAI Responses API를 호출해 텍스트를 생성한다.
     *
     * @param instructions AI 역할 지시문 (시스템 프롬프트)
     * @param input        처리할 텍스트 (에피소드 본문 등)
     * @return AI가 생성한 텍스트
     */
    public String generateText(String instructions, String input) {
        OpenAiRequestDto request = OpenAiRequestDto.builder()
                .model(openAiConfig.getModel())
                .instructions(instructions)
                .input(input)
                .build();

        try {
            OpenAiResponseDto response = openAiRestClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponseDto.class);

            return response.getFirstText();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 요약 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
