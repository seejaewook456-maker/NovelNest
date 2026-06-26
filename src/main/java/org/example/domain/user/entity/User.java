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

    @Builder
    private User(String email, String password, String nickname, Provider provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider != null ? provider : Provider.LOCAL;
        this.providerId = providerId;
    }
}
