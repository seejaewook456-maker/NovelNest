package org.example.global.security;

import org.example.domain.user.entity.Provider;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private static final String EMAIL = "user@example.com";

    private User activeUser() {
        return User.builder()
                .email(EMAIL)
                .password("encodedPassword")
                .nickname("홍길동")
                .provider(Provider.LOCAL)
                .build();
    }

    @Test
    void 정상_사용자는_인증_정보를_정상적으로_조회한다() {
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(activeUser()));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername(EMAIL);

        assertThat(userDetails.getUsername()).isEqualTo(EMAIL);
    }

    @Test
    void 탈퇴한_사용자는_인증_정보_조회에_실패한다() {
        // 탈퇴 회원은 findByEmailAndDeletedAtIsNull 조회 결과에 나타나지 않으므로,
        // 유효한 Access Token을 갖고 있어도 매 요청마다 인증이 거부된다 (JwtAuthenticationFilter → 401).
        given(userRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
