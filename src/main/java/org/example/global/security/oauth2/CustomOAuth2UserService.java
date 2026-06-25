package org.example.global.security.oauth2;

import lombok.RequiredArgsConstructor;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name  = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub"); // Google 고유 ID

        // LOCAL 계정과 동일 이메일 → 로그인 거부
        userRepository.findByEmail(email).ifPresent(existing -> {
            if (existing.getProvider() == Provider.LOCAL) {
                throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_already_exists"),
                    "해당 이메일은 이미 일반 가입된 계정입니다. 이메일과 비밀번호로 로그인해 주세요."
                );
            }
        });

        // GOOGLE 계정이 없으면 자동 회원가입
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
}
