package org.example.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    // 허용 Origin 목록을 환경별 설정 파일(application-local.yml, application-prod.yml)에서 주입
    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    // 독립된 CorsFilter 빈으로 등록하면 Spring Boot가 기본 우선순위(가장 낮음)로 서블릿 필터 체인에
    // 끼워 넣는데, Spring Security의 FilterChainProxy(order=-100)보다 뒤에 위치하게 된다.
    // 그 결과 authenticationEntryPoint/accessDeniedHandler가 401/403을 직접 응답하고 체인을
    // 더 진행시키지 않는 경우(SecurityConfig 참고) CorsFilter까지 도달하지 못해 CORS 헤더가
    // 누락되고, 브라우저가 이를 CORS 차단으로 표시한다.
    // CorsConfigurationSource만 빈으로 노출하고 SecurityConfig의 http.cors(...)에 연결하면,
    // Spring Security가 이 설정으로 자체 CorsFilter를 필터 체인 맨 앞에 배치해 인증 성공/실패
    // 여부와 무관하게 모든 응답에 CORS 헤더가 붙는다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Authorization 헤더 포함 요청 허용 (JWT 토큰 전송에 필요)
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
