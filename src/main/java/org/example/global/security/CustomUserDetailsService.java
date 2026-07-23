package org.example.global.security;

import lombok.RequiredArgsConstructor;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Spring Security가 인증 시 호출하는 메서드 (username = email)
    // 매 요청마다 호출되므로, 여기서 탈퇴 회원을 걸러내면 이미 발급된 Access Token이 만료 전이더라도
    // 탈퇴 이후에는 모든 보호 API에서 인증이 거부된다 (단순 서명/만료 검증을 넘어선 DB 상태 확인).
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        // Google 회원은 password가 null이므로 빈 문자열로 대체 (JWT 필터는 password를 검증하지 않음)
        String password = user.getPassword() != null ? user.getPassword() : "";
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                password,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
