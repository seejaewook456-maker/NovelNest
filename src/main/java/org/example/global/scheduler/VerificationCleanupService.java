package org.example.global.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.domain.emailverification.repository.EmailVerificationRepository;
import org.example.domain.passwordreset.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 벌크 삭제의 트랜잭션 경계를 담당하는 별도 빈.
// VerificationCleanupScheduler와 분리해 둔 이유는, 같은 클래스 안에서 @Transactional 메서드를 직접 호출하면
// 프록시를 거치지 않아 트랜잭션이 적용되지 않는 self-invocation 문제를 피하기 위함이다(@Async와 동일한 이유).
@Service
@RequiredArgsConstructor
public class VerificationCleanupService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Transactional
    public int cleanupEmailVerifications(LocalDateTime now) {
        return emailVerificationRepository.deleteExpired(now);
    }

    @Transactional
    public int cleanupPasswordResetTokens(LocalDateTime now) {
        return passwordResetTokenRepository.deleteExpiredOrUsed(now);
    }
}
