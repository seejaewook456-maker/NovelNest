package org.example.domain.airatelimit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// AI 전체 요청에 대한 분당 Rate Limit 설정값(application.yml의 app.ai.rate-limit.*).
// 기능별이 아니라 사용자당 "AI 전체 요청" 합계 기준으로 최근 windowSeconds초 동안 maxRequests회까지만
// 허용한다(회차 요약/설정 충돌 감지/등장인물 추출/세계관 추출/AI 챗봇 전부 포함).
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai.rate-limit")
public class AiRateLimitProperties {

    private int maxRequests = 10;
    private int windowSeconds = 60;
}
