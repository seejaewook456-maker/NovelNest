# 일회성 데이터 정리 정책 가이드

## 목적

이 프로젝트에서 만료되거나 한 번 쓰고 버려지는(휘발성) 데이터를 어떻게, 언제 정리하는지 한 곳에
정리한다. 대상은 아래 3가지다.

| 데이터 | 저장 위치 | 정리 방식 |
|---|---|---|
| 이메일 인증번호 (`email_verifications`) | 전용 테이블 | 스케줄러(`@Scheduled`, 매 정각) |
| Refresh Token | `users.refresh_token` 컬럼 | 정리 로직 불필요(즉시 교체/초기화 구조) |
| AI 분당 Rate Limit 요청 로그 (`ai_request_logs`) | 전용 테이블 | 요청 시점마다 함께 정리 |

세 데이터 모두 "정리 로직이 없어서 문제"인 경우는 없으며, 각자 데이터 특성에 맞는 방식을 이미 쓰고
있다. 이 문서는 신규 기능 추가가 아니라 **기존 구현 현황을 점검하고 문서화**한 결과다.

---

## 1. 이메일 인증번호 (`EmailVerification`)

### 삭제 시점

- **인증 성공 직후**: 레코드를 바로 지우지 않고 `verified=true`로 마킹만 한다
  (`EmailVerificationService.verifyCode()`). 회원가입(`UserService.signup()`)이 `assertVerified()`로
  이 상태를 다시 확인해야 하기 때문에, verify와 실제 소비(삭제) 시점을 분리해 둔 구조다.
- **실제 소비(삭제) 시점**: 회원가입이 완료된 직후 `EmailVerificationService.consume(email)` →
  `EmailVerificationRepository.deleteByEmailAndPurpose(email, purpose)`로 삭제한다(동일 인증번호
  재사용 방지). 비밀번호 재설정도 동일한 목적(`Purpose.PASSWORD_RESET`)별로 독립 관리된다.
- **재발급(재전송) 시**: 기존 레코드를 지우고 새로 만들지 않고, `EmailVerification.renew()`로
  같은 행의 `verificationCode`/`expiresAt`/`verified` 상태를 갱신한다. `(email, purpose)` 복합
  유니크 제약(`uk_email_verifications_email_purpose`)이 있어 애초에 중복 행이 생길 수 없다.
- **만료된 인증번호**: 회원가입/비밀번호 재설정을 끝까지 완료하지 않고 이탈한 경우, 만료된 행이
  계속 남을 수 있어 별도 정리가 필요하다 — 아래 스케줄러가 담당한다.

### 정리 정책 (스케줄러)

- `VerificationCleanupScheduler`(`src/main/java/org/example/global/scheduler/VerificationCleanupScheduler.java`)가
  `@Scheduled(cron = "${app.cleanup.verification-cron}")`로 **매 정각(1시간마다)** 실행된다
  (`application.yml`: `app.cleanup.verification-cron: "0 0 * * * *"`).
- 실제 삭제는 `VerificationCleanupService.cleanupEmailVerifications(now)` →
  `EmailVerificationRepository.deleteExpired(now)`(`expires_at < now` 벌크 삭제)가 수행한다.
- 같은 스케줄러가 `password_reset_tokens`(비밀번호 재설정 임시 토큰)의 만료/사용 완료 건도 함께
  정리한다(`PasswordResetTokenRepository.deleteExpiredOrUsed`).
- 정리 작업 실패는 다른 정리 작업에 영향을 주지 않도록 각각 `try/catch`로 독립 처리되며, 실패 시
  Sentry로 캡처된다.

### 결론

인증 성공 시 삭제(소비 시점 분리는 의도된 설계), 재발급 시 기존 행 갱신(중복 없음), 만료 정리
스케줄러까지 이미 모두 구현되어 있다. **변경하지 않았다.**

---

## 2. Refresh Token

### 저장 방식

별도 테이블이 아니라 `User` 엔티티의 `refresh_token` 컬럼(`VARCHAR(512)`, `V2__add_refresh_token.sql`)
하나로 관리한다. 사용자 1명당 최대 1개의 값만 존재할 수 있는 구조라, "만료된 토큰이 누적된다"는
개념 자체가 성립하지 않는다.

### 삭제/교체 시점

- **로그아웃**: `UserService.logout()` → `User.clearRefreshToken()`으로 즉시 `null` 처리.
- **회원 탈퇴**: `User.withdraw()`에서도 함께 `null` 처리.
- **재발급(Refresh Token Rotation)**: `UserService.reissue()`가 재발급마다 새 Refresh Token을 발급해
  `User.updateRefreshToken()`으로 기존 값을 덮어쓴다. 요청받은 토큰이 DB에 저장된 값과 다르면
  `REFRESH_TOKEN_MISMATCH`로 거부한다 — 탈취된 이전 토큰이 재사용되더라도 정상 사용자가 먼저
  재발급받으면 즉시 무효화된다.
- **자체 만료**: JWT의 `exp` 클레임으로 유효기간이 관리되므로, 만료된 토큰으로 재발급을 시도하면
  `REFRESH_TOKEN_EXPIRED`로 거부된다. DB 컬럼 값 자체는 로그아웃/재발급 전까지 남아있을 수 있지만
  이미 무효한 토큰이라 보안·저장공간 문제가 되지 않는다.

### 별도 정리 로직이 필요 없는 이유

`VerificationCleanupScheduler`의 주석에도 명시되어 있듯, 컬럼 하나로 관리되고 로그아웃/탈퇴 시점에
즉시 초기화되는 구조라 배치성 정리 대상에서 제외했다.

### 결론

로그아웃 시 삭제, 재발급 시 교체(rotation), 자체 만료 검증까지 이미 모두 구현되어 있고, 구조상
"누적"이 발생할 수 없다. **변경하지 않았다.**

---

## 3. AI 분당 Rate Limit 요청 로그 (`AiRequestLog`)

AI 분당 Rate Limit 기능 자체가 이미 구현되어 있다(2026-07-29, `AiRateLimitService`). 자세한 정책·
동시성 제어는 [AI_RATE_LIMIT.md](./AI_RATE_LIMIT.md) 참고.

### 삭제 시점 / 정책

- 별도 `@Scheduled` 배치를 두지 않고, **`AiRateLimitService.checkAndRecord()` 호출(=매 AI 요청)
  시마다 함께 정리**한다.
- `aiRequestLogRepository.deleteByRequestedAtBefore(windowStart)` — `windowStart`(현재 시각 - 60초)
  이전 로그를 전체 사용자 기준으로 일괄 삭제한다.
- 윈도우(최근 60초) 밖으로 나간 로그는 어떤 사용자의 Rate Limit 판단에도 더 이상 쓰이지 않으므로,
  테이블 크기는 "활성 사용자 수 x 사용자당 최대 요청 수(10)" 규모로 자연스럽게 유지된다.

### 결론

AI 분당 요청 제한 기능과 오래된 로그 삭제 로직 모두 이미 구현되어 있다. **변경하지 않았다.**
(요청 지침의 "구현되어 있지 않다면 삭제 기능도 만들지 않는다" / "이미 있다면 유지한다" 조건 중,
후자에 해당한다.)

---

## 관련 문서

- [AI_RATE_LIMIT.md](./AI_RATE_LIMIT.md) — AI 분당 Rate Limit 정책·동시성 제어·요청 로그 정리 상세
- [AI_USAGE_LIMIT.md](./AI_USAGE_LIMIT.md) — AI 하루 사용량 제한(정리 대상 아님, 날짜별 이력으로 보존)
