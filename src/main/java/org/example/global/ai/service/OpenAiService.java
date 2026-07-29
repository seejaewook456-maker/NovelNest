package org.example.global.ai.service;

import io.sentry.Sentry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.global.ai.config.OpenAiConfig;
import org.example.global.ai.dto.OpenAiRequestDto;
import org.example.global.ai.dto.OpenAiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
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
        return generateText(instructions, input, null);
    }

    /**
     * 최대 출력 토큰을 지정해 OpenAI Responses API를 호출한다.
     *
     * @param maxOutputTokens 응답 최대 출력 토큰. null이면 제한을 지정하지 않는다.
     */
    public String generateText(String instructions, String input, Integer maxOutputTokens) {
        OpenAiRequestDto request = OpenAiRequestDto.builder()
                .model(openAiConfig.getModel())
                .instructions(instructions)
                .input(input)
                .maxOutputTokens(maxOutputTokens)
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
            // 타임아웃/연결 실패/OpenAI 5xx 등 예상하지 못한 외부 API 오류 — 원인(e)을 그대로 캡처한다.
            // 이후 IllegalArgumentException으로 감싸 400으로 응답하는 기존 동작은 변경하지 않는다
            // (여기서 캡처하는 것은 원본 예외 e이므로, 감싼 뒤 다시 던지는 IllegalArgumentException은
            //  400으로 처리되어 GlobalExceptionHandler에서 별도로 캡처되지 않는다 — 중복 전송 없음).
            log.error("OpenAI API call failed. model={}", openAiConfig.getModel(), e);
            Sentry.captureException(e);
            throw new IllegalArgumentException("AI 요약 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
