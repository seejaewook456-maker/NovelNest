package org.example.domain.passwordreset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 비밀번호 재설정 인증번호 검증에 성공한 사용자에게만 발급되는 일회성 임시 토큰.
// AccessToken/RefreshToken(JWT)과 완전히 분리된 opaque 랜덤 토큰이며, 원문은 저장하지 않고 SHA-256 해시만 저장한다.
@Entity
@Table(
    name = "password_reset_tokens",
    indexes = {
        @Index(name = "uk_password_reset_tokens_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_password_reset_tokens_expires_at", columnList = "expires_at"),
        @Index(name = "idx_password_reset_tokens_email", columnList = "email")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private PasswordResetToken(String email, String tokenHash, LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.used = false;
        this.createdAt = createdAt;
    }

    public static PasswordResetToken create(String email, String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        return PasswordResetToken.builder()
                .email(email)
                .tokenHash(tokenHash)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }
}
