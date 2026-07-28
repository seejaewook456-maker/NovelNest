package org.example.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.policy.UserRejoinPolicy;
import org.example.domain.user.repository.UserRepository;
import org.example.global.exception.BusinessException;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserRejoinPolicy userRejoinPolicy;

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

    // 테스트에서 super.loadUser()의 실제 HTTP 호출 없이 이 로직만 검증할 수 있도록 default 접근 제한자 유지
    OAuth2User processGoogleUser(OAuth2User oAuth2User) {
        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");

        // 1. 활성 회원인지 확인
        Optional<User> activeUser = userRepository.findByEmailAndDeletedAtIsNull(email);

        if (activeUser.isPresent()) {
            // 동일 이메일로 다른 Provider가 가입되어 있으면 거부
            if (activeUser.get().getProvider() != Provider.GOOGLE) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_already_exists"),
                    "해당 이메일은 이미 다른 방법으로 가입된 계정입니다."
                );
            }
            log.info("Google OAuth2 login. userId={}", activeUser.get().getId());
            return oAuth2User;
        }

        // 2~3. 탈퇴 회원인지 확인 + 탈퇴 후 재가입 제한 기간이 지났는지 확인
        assertRejoinAllowedForOAuth(
                userRepository.findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(email));

        // 재가입인 경우에도 기존 User를 복구하지 않고 새 User를 생성한다.
        User newUser = userRepository.save(
            User.builder()
                .email(email)
                .nickname(name != null ? name : email)
                .provider(Provider.GOOGLE)
                .providerId(providerId)
                .build()
        );
        log.info("Google OAuth2 signup. userId={}", newUser.getId());

        return oAuth2User;
    }

    // ── Kakao ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    OAuth2User processKakaoUser(OAuth2User oAuth2User, String nameAttributeKey) {
        String providerId = String.valueOf((Object) oAuth2User.getAttribute("id"));

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

        // 1. 활성 회원인지 확인 (provider+providerId 기준)
        Optional<User> activeUser = userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(Provider.KAKAO, providerId);

        User user;
        if (activeUser.isPresent()) {
            user = activeUser.get();
            log.info("Kakao OAuth2 login. userId={}", user.getId());
        } else {
            // 2~3. 탈퇴 회원인지 확인 + 탈퇴 후 재가입 제한 기간이 지났는지 확인
            assertRejoinAllowedForOAuth(userRepository
                    .findTopByProviderAndProviderIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Provider.KAKAO, providerId));

            // 실제 이메일이 있을 때만 다른 Provider의 활성 계정과 충돌하는지 확인
            if (finalEmail != null && !finalEmail.isBlank()) {
                userRepository.findByEmailAndDeletedAtIsNull(finalEmail).ifPresent(existing -> {
                    if (existing.getProvider() != Provider.KAKAO) {
                        throw new OAuth2AuthenticationException(
                            new OAuth2Error("email_already_exists"),
                            "해당 이메일은 이미 다른 방법으로 가입된 계정입니다."
                        );
                    }
                });
            }

            // 재가입인 경우에도 기존 User를 복구하지 않고 새 User를 생성한다.
            user = userRepository.save(
                User.builder()
                    .email(resolvedEmail)
                    .nickname(finalNickname)
                    .provider(Provider.KAKAO)
                    .providerId(providerId)
                    .build()
            );
            log.info("Kakao OAuth2 signup. userId={}", user.getId());
        }

        // SuccessHandler가 getAttribute("email")로 JWT를 발급하므로
        // DB에 저장된 email을 표준 속성으로 주입하여 반환
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put("email", user.getEmail());
        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, nameAttributeKey);
    }

    // UserRejoinPolicy는 BusinessException을 던지지만, OAuth2 인증 흐름은 AuthenticationException 계열만
    // 처리(OAuth2AuthenticationFailureHandler)하므로 여기서 변환해준다. "withdrawn_user", "email_already_exists"와
    // 같은 방식으로 프론트가 error 쿼리 파라미터로 그대로 받아 안내 문구에 사용할 수 있다.
    private void assertRejoinAllowedForOAuth(Optional<User> latestWithdrawnUser) {
        try {
            userRejoinPolicy.assertRejoinAllowed(latestWithdrawnUser);
        } catch (BusinessException e) {
            throw new OAuth2AuthenticationException(new OAuth2Error("rejoin_blocked"), e.getMessage());
        }
    }
}
