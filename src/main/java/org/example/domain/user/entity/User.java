package org.example.domain.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.global.common.BaseEntity;

@Entity
@Table(
    name = "users",
    indexes = {
        // OAuth 로그인 시 findByProviderAndProviderId 쿼리 성능 보장
        @Index(name = "idx_users_provider_provider_id", columnList = "provider, provider_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column // Google 회원은 null
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column
    private String providerId; // Google sub ID, LOCAL이면 null

    // 향후 Premium 구독 도입을 위한 플랜 필드
    // 현재 FREE는 저장 제한이 없는 상태이며, 제한은 향후 구현 예정
    // columnDefinition의 DEFAULT 'FREE'는 기존 DB 행에 컬럼 추가 시 NULL 없이 마이그레이션되도록 보장
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'FREE'")
    private Plan plan = Plan.FREE;

    // 이메일 소유 인증 완료 여부.
    // - LOCAL 회원가입: EmailVerificationService의 인증 통과 후에만 User가 생성되므로 항상 true로 저장
    // - GOOGLE/KAKAO 회원가입: OAuth 제공자가 이메일 소유를 이미 검증한 것으로 간주하여 true로 저장
    // columnDefinition의 DEFAULT TRUE는 이메일 인증 기능 도입 이전에 가입된 기존 계정이
    // 컬럼 추가 후에도 로그인 불가 상태가 되지 않도록 보장
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean emailVerified;

    // Refresh Token 재발급 API 검증용 — 로그인/재발급 시 갱신하고, 로그아웃 시 null로 초기화
    @Column(length = 512)
    private String refreshToken;

    @Builder
    private User(String email, String password, String nickname, Provider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider != null ? provider : Provider.LOCAL;
        this.providerId = providerId;
        this.plan = Plan.FREE; // 모든 신규 회원은 FREE로 시작
        this.emailVerified = true; // User는 이메일 인증(또는 OAuth 검증) 완료 후에만 생성됨
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void clearRefreshToken() {
        this.refreshToken = null;
    }
}
