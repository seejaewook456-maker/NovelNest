package org.example.global.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VerificationCleanupSchedulerTest {

    @Mock
    private VerificationCleanupService verificationCleanupService;

    @InjectMocks
    private VerificationCleanupScheduler scheduler;

    @Test
    void 두_정리_작업을_모두_호출한다() {
        given(verificationCleanupService.cleanupEmailVerifications(any())).willReturn(0);
        given(verificationCleanupService.cleanupPasswordResetTokens(any())).willReturn(0);

        scheduler.cleanupExpiredVerificationData();

        verify(verificationCleanupService).cleanupEmailVerifications(any(LocalDateTime.class));
        verify(verificationCleanupService).cleanupPasswordResetTokens(any(LocalDateTime.class));
    }

    @Test
    void 이메일인증_정리가_실패해도_토큰_정리는_계속_실행된다() {
        given(verificationCleanupService.cleanupEmailVerifications(any()))
                .willThrow(new RuntimeException("DB 오류"));
        given(verificationCleanupService.cleanupPasswordResetTokens(any())).willReturn(0);

        scheduler.cleanupExpiredVerificationData();

        verify(verificationCleanupService).cleanupPasswordResetTokens(any(LocalDateTime.class));
    }

    @Test
    void 데이터가_없어도_정상_종료된다() {
        given(verificationCleanupService.cleanupEmailVerifications(any())).willReturn(0);
        given(verificationCleanupService.cleanupPasswordResetTokens(any())).willReturn(0);

        scheduler.cleanupExpiredVerificationData();
    }
}
