package org.example.global.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

// 만료되었거나 이미 사용이 끝난 일회성 인증 데이터를 주기적으로 정리한다.
// RefreshToken은 별도 테이블이 아니라 User.refreshToken 단일 컬럼으로 관리되며,
// 로그아웃/비밀번호 변경 시점에 즉시 null로 초기화되므로 이 스케줄러의 정리 대상에서 제외한다.
//
// 주의: 여러 서버 인스턴스로 확장될 경우 이 스케줄러는 인스턴스마다 각각 실행된다(분산 락 미적용).
// 삭제 쿼리 자체는 만료/사용 완료 조건만 보고 동작하는 멱등 연산이라 중복 실행되어도 데이터 정합성 문제는 없다.
@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationCleanupScheduler {

    private final VerificationCleanupService verificationCleanupService;

    @Scheduled(cron = "${app.cleanup.verification-cron}")
    public void cleanupExpiredVerificationData() {
        LocalDateTime now = LocalDateTime.now();

        // 하나의 정리 작업이 실패해도 다른 정리 작업은 계속 실행되도록 각각 독립적으로 try/catch 처리
        try {
            int deleted = verificationCleanupService.cleanupEmailVerifications(now);
            log.info("Expired email verification cleanup done. deletedCount={}", deleted);
        } catch (Exception e) {
            log.error("Failed to clean up expired email verifications. reason={}", e.getMessage());
        }

        try {
            int deleted = verificationCleanupService.cleanupPasswordResetTokens(now);
            log.info("Expired/used password reset token cleanup done. deletedCount={}", deleted);
        } catch (Exception e) {
            log.error("Failed to clean up password reset tokens. reason={}", e.getMessage());
        }
    }
}
