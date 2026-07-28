package org.example.global.security.oauth2;

import org.example.domain.user.config.RejoinPolicyProperties;
import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.policy.UserRejoinPolicy;
import org.example.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
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

    private CustomOAuth2UserService customOAuth2UserService;

    private static final String EMAIL = "user@example.com";
    private static final int BLOCK_DAYS = 14;

    // UserRejoinPolicy는 날짜 계산 로직 자체를 검증해야 하므로 Mock이 아닌 실제 객체를 사용한다.
    @BeforeEach
    void setUp() {
        RejoinPolicyProperties properties = new RejoinPolicyProperties();
        properties.setBlockDays(BLOCK_DAYS);
        UserRejoinPolicy userRejoinPolicy = new UserRejoinPolicy(properties);
        customOAuth2UserService = new CustomOAuth2UserService(userRepository, userRejoinPolicy);
    }

    private User activeUser(Provider provider, String providerId) {
        return User.builder()
                .email(EMAIL)
                .nickname("홍길동")
                .provider(provider)
                .providerId(providerId)
                .build();
    }

    private User withdrawnUser(Provider provider, String providerId, LocalDateTime deletedAt) {
        User user = activeUser(provider, providerId);
        user.withdraw();
        ReflectionTestUtils.setField(user, "deletedAt", deletedAt);
        return user;
    }

    private OAuth2User googleOAuth2User() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", EMAIL, "name", "홍길동", "sub", "google-sub-id"),
                "sub"
        );
    }

    private OAuth2User kakaoOAuth2User() {
        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of(
                        "id", 12345L,
                        "kakao_account", Map.of("email", EMAIL, "profile", Map.of("nickname", "닉네임"))
                ),
                "id"
        );
    }

    // ===== Google =====

    @Test
    void 활성_회원이면_구글_로그인에_성공한다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL))
                .willReturn(Optional.of(activeUser(Provider.GOOGLE, "google-sub-id")));

        assertThatCode(() -> customOAuth2UserService.processGoogleUser(googleOAuth2User()))
                .doesNotThrowAnyException();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 탈퇴_후_7일이면_구글_재가입이_차단된다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.empty());
        given(userRepository.findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(EMAIL))
                .willReturn(Optional.of(withdrawnUser(Provider.GOOGLE, "google-sub-id", LocalDateTime.now().minusDays(7))));

        assertThatThrownBy(() -> customOAuth2UserService.processGoogleUser(googleOAuth2User()))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 탈퇴_후_14일이_지나면_구글_신규_회원가입에_성공한다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.empty());
        given(userRepository.findTopByEmailAndDeletedAtIsNotNullOrderByDeletedAtDesc(EMAIL))
                .willReturn(Optional.of(withdrawnUser(Provider.GOOGLE, "google-sub-id", LocalDateTime.now().minusDays(15))));
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> customOAuth2UserService.processGoogleUser(googleOAuth2User()))
                .doesNotThrowAnyException();

        verify(userRepository).save(any(User.class));
    }

    // ===== Kakao =====

    @Test
    void 활성_회원이면_카카오_로그인에_성공한다() {
        given(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(Provider.KAKAO, "12345"))
                .willReturn(Optional.of(activeUser(Provider.KAKAO, "12345")));

        assertThatCode(() -> customOAuth2UserService.processKakaoUser(kakaoOAuth2User(), "id"))
                .doesNotThrowAnyException();

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 탈퇴_후_7일이면_카카오_재가입이_차단된다() {
        given(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(Provider.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findTopByProviderAndProviderIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Provider.KAKAO, "12345"))
                .willReturn(Optional.of(withdrawnUser(Provider.KAKAO, "12345", LocalDateTime.now().minusDays(7))));

        assertThatThrownBy(() -> customOAuth2UserService.processKakaoUser(kakaoOAuth2User(), "id"))
                .isInstanceOf(OAuth2AuthenticationException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void 탈퇴_후_14일이_지나면_카카오_신규_회원가입에_성공한다() {
        given(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(Provider.KAKAO, "12345"))
                .willReturn(Optional.empty());
        given(userRepository.findTopByProviderAndProviderIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(Provider.KAKAO, "12345"))
                .willReturn(Optional.of(withdrawnUser(Provider.KAKAO, "12345", LocalDateTime.now().minusDays(15))));
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> customOAuth2UserService.processKakaoUser(kakaoOAuth2User(), "id"))
                .doesNotThrowAnyException();

        verify(userRepository).save(any(User.class));
    }
}
