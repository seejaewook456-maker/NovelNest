package org.example.global.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    // OpenAI API 호출용 RestClient 빈 — baseUrl과 인증 헤더를 미리 설정해둬서
    // OpenAiService에서 uri("/responses") 만 붙여 쓸 수 있다
    @Bean
    public RestClient openAiRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public String getModel() {
        return model;
    }
}
