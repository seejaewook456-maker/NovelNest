package org.example.domain.emailverification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "email_verifications",
    indexes = {
        @Index(name = "idx_email_verifications_email", columnList = "email", unique = true)
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String verificationCode;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 인증번호 발송 시각. 재전송 시 최신 발송 시각으로 갱신되어 재전송 제한(60초) 기준으로 사용됨
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime verifiedAt;

    @Builder
    private EmailVerification(String email, String verificationCode, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.email = email;
        this.verificationCode = verificationCode;
        this.verified = false;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static EmailVerification create(String email, String verificationCode, LocalDateTime now, LocalDateTime expiresAt) {
        return EmailVerification.builder()
                .email(email)
                .verificationCode(verificationCode)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    // 동일 이메일 재요청 시 기존 인증번호를 새 값으로 갱신 (레코드는 유지, 새 row 생성 안 함)
    public void renew(String verificationCode, LocalDateTime now, LocalDateTime expiresAt) {
        this.verificationCode = verificationCode;
        this.createdAt = now;
        this.expiresAt = expiresAt;
        this.verified = false;
        this.verifiedAt = null;
    }

    public void markVerified(LocalDateTime now) {
        this.verified = true;
        this.verifiedAt = now;
    }

    public boolean matchesCode(String code) {
        return this.verificationCode.equals(code);
    }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    public boolean isResendTooSoon(LocalDateTime now, long intervalSeconds) {
        return Duration.between(createdAt, now).getSeconds() < intervalSeconds;
    }
}
