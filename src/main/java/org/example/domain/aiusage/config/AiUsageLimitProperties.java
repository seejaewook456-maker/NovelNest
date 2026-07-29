package org.example.domain.aiusage.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// AI 기능별 하루 사용량 제한값(application.yml의 app.ai.usage.daily-limit.*).
// 운영 중 코드 수정 없이 조정할 수 있도록 환경변수(AI_DAILY_*_LIMIT)로 오버라이드 가능하며,
// 값이 없으면 여기 적힌 기본값을 사용한다. 전체 합계 제한은 두지 않고 기능별 제한만 적용한다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai.usage.daily-limit")
public class AiUsageLimitProperties {

    private int summary = 20;
    private int conflict = 15;
    private int character = 15;
    private int worldview = 15;
    private int chat = 20;
}
