package org.example.global.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    // 환경별 설정 파일(application-local.yml, application-prod.yml)에서 주입
    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String errorMessage;

        // 카카오/구글 동의 화면에서 사용자가 뒤로가기·취소를 누르면 provider가 error=access_denied로
        // 리다이렉트하는데, 이는 실패가 아니라 정상적인 취소 흐름이다. 그대로 두면 Spring Security가
        // 만든 영문 원문 메시지("...access denied..." 등)가 그대로 화면에 노출되어 진짜 오류처럼 보인다.
        if (exception instanceof OAuth2AuthenticationException oAuth2Exception
                && "access_denied".equals(oAuth2Exception.getError().getErrorCode())) {
            log.info("OAuth2 login cancelled by user. errorCode={}", oAuth2Exception.getError().getErrorCode());
            errorMessage = URLEncoder.encode("로그인이 취소되었습니다.", StandardCharsets.UTF_8);
        } else {
            log.warn("OAuth2 authentication failed. error={}", exception.getMessage());
            errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        }

        getRedirectStrategy().sendRedirect(request, response,
            frontendBaseUrl + "/login?error=" + errorMessage);
    }
}
