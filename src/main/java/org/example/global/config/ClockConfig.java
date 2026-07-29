package org.example.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

// 서버/DB/Docker 컨테이너의 기본 시간대가 UTC이더라도, "오늘" 판단이 필요한 로직(AI 하루 사용량 등)은
// 항상 Asia/Seoul 기준 날짜를 사용해야 한다. LocalDate.now()를 코드 곳곳에서 직접 호출하는 대신
// 이 Clock 빈을 주입받아 LocalDate.now(clock)으로 사용하면, 테스트에서 Clock.fixed(...)로 교체해
// 날짜 경계(자정 전후) 동작을 실제 시각에 의존하지 않고 검증할 수 있다.
@Configuration
public class ClockConfig {

    public static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    public Clock clock() {
        return Clock.system(SEOUL_ZONE);
    }
}
