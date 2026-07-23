package org.example.global.security.oauth2;

import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// CustomOAuth2UserService.loadUser()는 super.loadUser()에서 실제 provider(Google/Kakao)로 HTTP 호출을
// 하므로 단위 테스트로 직접 검증하기 어렵다. 대신 provider 응답을 처리하는 processGoogleUser/
// processKakaoUser(테스트 검증을 위해 default 접근 제한자로 열어둠)를 OAuth2User를 직접 구성해 호출한다.
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private static final String EMAIL = "user@example.com";

    private User withdrawnUser(Provider provider, String providerId) {
        User user = User.builder()
                .email(EMAIL)
                .nickname("홍길동")
                .provider(provider)
                .providerId(providerId)
                .build();
        user.withdraw();
        return user;
    }

    @Test
    void 탈퇴한_계정과_동일한_이메일로_구글_로그인을_시도하면_차단된다() {
        OAuth2User googleUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", EMAIL, "name", "홍길동", "sub", "google-sub-id"),
                "sub"
        );
        given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(withdrawnUser(Provider.GOOGLE, "google-sub-id")));

        assertThatThrownBy(() -> customOAuth2UserService.processGoogleUser(googleUser))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 탈퇴한_계정과_동일한_providerId로_카카오_로그인을_시도하면_차단된다() {
        OAuth2User kakaoUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "id", 12345L,
                        "kakao_account", Map.of("email", EMAIL, "profile", Map.of("nickname", "닉네임"))
                ),
                "id"
        );
        given(userRepository.findByProviderAndProviderId(Provider.KAKAO, "12345"))
                .willReturn(Optional.of(withdrawnUser(Provider.KAKAO, "12345")));

        assertThatThrownBy(() -> customOAuth2UserService.processKakaoUser(kakaoUser, "id"))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).save(any(User.class));
    }
}
