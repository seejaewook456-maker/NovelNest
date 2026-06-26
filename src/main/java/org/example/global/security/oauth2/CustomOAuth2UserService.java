package org.example.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        if ("kakao".equals(registrationId)) {
            return processKakaoUser(oAuth2User, nameAttributeKey);
        }
        return processGoogleUser(oAuth2User);
    }

    // ── Google ──────────────────────────────────────────────────────────────

    private OAuth2User processGoogleUser(OAuth2User oAuth2User) {
        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        // 동일 이메일로 다른 Provider가 가입되어 있으면 거부
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getProvider() != Provider.GOOGLE) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_already_exists"),
                    "해당 이메일은 이미 다른 방법으로 가입된 계정입니다."
                );
            }
        });

        userRepository.findByEmail(email).orElseGet(() ->
            userRepository.save(
                User.builder()
                    .email(email)
                    .nickname(name != null ? name : email)
                    .provider(Provider.GOOGLE)
                    .providerId(providerId)
                    .build()
            )
        );

        return oAuth2User;
    }

    // ── Kakao ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private OAuth2User processKakaoUser(OAuth2User oAuth2User, String nameAttributeKey) {
        String providerId = String.valueOf(oAuth2User.getAttribute("id"));

        // 카카오 응답에서 이메일 / 닉네임 파싱 (중첩 구조)
        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        String email    = null;
        String nickname = "카카오사용자_" + providerId;

        if (kakaoAccount != null) {
            email = (String) kakaoAccount.get("email");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            if (profile != null && profile.get("nickname") != null) {
                nickname = (String) profile.get("nickname");
            }
        }

        // 이메일이 없으면 provider+providerId 기반 합성 식별자 사용
        // (이후 서비스/JWT 로직이 email 기반이므로 null 대신 합성값 저장)
        final String resolvedEmail = (email != null && !email.isBlank())
                ? email
                : "kakao_" + providerId + "@kakao.local";

        final String finalEmail    = email;
        final String finalNickname = nickname;

        // provider+providerId 기준으로 기존 회원 조회 → 없으면 자동 회원가입
        User user = userRepository.findByProviderAndProviderId(Provider.KAKAO, providerId)
                .orElseGet(() -> {
                    // 실제 이메일이 있을 때만 다른 Provider 충돌 확인
                    if (finalEmail != null && !finalEmail.isBlank()) {
                        userRepository.findByEmail(finalEmail).ifPresent(existing -> {
                            if (existing.getProvider() != Provider.KAKAO) {
                                throw new OAuth2AuthenticationException(
                                    new OAuth2Error("email_already_exists"),
                                    "해당 이메일은 이미 다른 방법으로 가입된 계정입니다."
                                );
                            }
                        });
                    }
                    return userRepository.save(
                        User.builder()
                            .email(resolvedEmail)
                            .nickname(finalNickname)
                            .provider(Provider.KAKAO)
                            .providerId(providerId)
                            .build()
                    );
                });

        // SuccessHandler가 getAttribute("email")로 JWT를 발급하므로
        // DB에 저장된 email을 표준 속성으로 주입하여 반환
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", user.getEmail());
        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, nameAttributeKey);
    }
}
