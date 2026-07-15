package org.example.global.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.global.security.JwtAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.example.global.security.oauth2.CustomOAuth2UserService;
import org.example.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import org.example.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsUtils;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2FailureHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // X-Content-Type-Options, X-Frame-Options(DENY)는 Spring Security 기본값으로 적용됨
            // Referrer-Policy만 명시적으로 추가
            .headers(headers -> headers
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            // OAuth2 state 파라미터 저장을 위해 IF_REQUIRED 사용 (JWT 요청에는 세션 미사용)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            // IF_REQUIRED이므로 OAuth2 로그인 과정에서 세션이 생성될 수 있는데, 기본 설정이라면
            // SecurityContextHolderFilter가 그 세션에 인증 정보를 함께 저장/복원해버린다.
            // 그 결과 JwtAuthenticationFilter가 Access Token 검증에 실패해도, 세션에 남은 인증이
            // 그대로 통과되어 로그아웃/Refresh Token 무효화 이후에도 요청이 인증돼버리는 문제가 있었다.
            // SecurityContext는 매 요청마다 JwtAuthenticationFilter가 새로 채우는 것만 사용하도록
            // RequestAttributeSecurityContextRepository로 한정해, 세션에 저장/복원되지 않게 한다.
            // (OAuth2 state 저장은 HttpSessionOAuth2AuthorizationRequestRepository가 이 설정과
            //  무관하게 세션을 직접 사용하므로 영향받지 않는다.)
            .securityContext(securityContext ->
                securityContext.securityContextRepository(new RequestAttributeSecurityContextRepository())
            )
            .authorizeHttpRequests(auth -> auth
                // CORS preflight(OPTIONS)는 인증 정보 없이 오므로, 인증 필요 경로보다 먼저 무조건 허용
                // (그렇지 않으면 인증 필터가 401을 먼저 반환해 CorsFilter까지 도달하지 못하고 브라우저가 CORS 오류로 처리함)
                .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
                .requestMatchers(
                        "/health", "/error",
                        "/api/users/signup", "/api/users/login",
                        "/api/users/email/send-code", "/api/users/email/verify-code",
                        "/api/auth/refresh", // Access Token 만료 후 호출되므로 인증 없이 허용
                        "/oauth2/**", "/login/oauth2/**",
                        "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**", "/api-docs/**"
                ).permitAll()
                // 비밀번호를 잊은 사용자가 로그인 없이 호출해야 하므로 3개 POST 경로만 명시적으로 허용
                // (재설정 토큰은 요청 바디로 전달되어 JwtAuthenticationFilter가 파싱하지 않으므로 별도 처리 불필요)
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/password-reset/code",
                        "/api/auth/password-reset/verify",
                        "/api/auth/password-reset/confirm"
                ).permitAll()
                .anyRequest().authenticated()
            )
            // 인증/권한 오류 시 HTML 대신 JSON 반환 (필터 레벨에서 처리 — GlobalExceptionHandler 도달 전)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                    );
                })
                .accessDeniedHandler((request, response, e) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write(
                        "{\"success\":false,\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"data\":null}"
                    );
                })
            )
            // Google OAuth2 로그인 설정
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler)
            )
            // UsernamePasswordAuthenticationFilter 이전에 JWT 필터 실행
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
