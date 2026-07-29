# AI 분당 Rate Limit 가이드

## 목적

하루 사용량 제한([AI_USAGE_LIMIT.md](./AI_USAGE_LIMIT.md))만으로는 짧은 시간에 몰아서 요청을 보내는
남용(예: 스크립트로 1분 안에 수십 번 연속 호출)을 막을 수 없다. 이를 막기 위해 사용자별로 **최근
60초 동안 최대 10회**까지만 AI 요청을 허용하는 분당 Rate Limit을 추가한다.

---

## 1. 정책

- **기준**: 사용자당 **최근 60초(Sliding Window)** 동안 **최대 10회**.
- 고정된 분 단위(매 정각 00~59초)가 아니라, 요청이 들어온 시점 기준으로 항상 "지금으로부터 60초 전"까지
  슬라이딩하며 계산한다. 예: 12:30:15에 요청했다면 12:31:15가 되는 순간 이 요청은 더 이상 카운트되지
  않는다.
- **기능별이 아니라 "AI 전체 요청" 합계 기준이다.** 회차 요약/설정 충돌 감지/등장인물 추출/세계관 추출/
  AI 챗봇 5개 기능을 모두 합쳐서 센다. 예: 챗봇 5회 + 회차 요약 3회 + 세계관 추출 2회 = 10회를 채우면,
  11번째 요청은 어떤 기능이든 차단된다.
- 제한값은 `AiRateLimitProperties`(`app.ai.rate-limit.*`) 한 곳에서만 관리한다.

---

## 2. 백엔드 구조

### 도메인 (`org.example.domain.airatelimit`)

| 파일 | 역할 |
|---|---|
| `entity/AiRequestLog.java` | 사용자별 AI 요청 시각 로그. `id`/`user_id`/`requested_at` 최소 컬럼만 가진다(BaseEntity 미상속 — 수정되지 않는 휘발성 데이터라 createdAt/updatedAt이 불필요) |
| `repository/AiRequestLogRepository.java` | 최근 N초 요청 수 COUNT 1개 + 윈도우 밖 오래된 로그 DELETE 1개 |
| `config/AiRateLimitProperties.java` | 제한값(`app.ai.rate-limit.max-requests`, `app.ai.rate-limit.window-seconds`) 바인딩 |
| `service/AiRateLimitService.java` | 검사·기록을 담당하는 단일 창구. `checkAndRecord(userId)` |

### 각 AI 서비스와의 연동

`EpisodeSummaryService`, `ConflictDetectionService`, `CharacterExtractionService`,
`WorldSettingExtractionService`, `ChatService` 각각에 `AiRateLimitService`를 주입받아, 기존
`aiUsageService.checkAndIncrement(...)` **바로 앞줄**에 `aiRateLimitService.checkAndRecord(user.getId())`
한 줄만 추가했다.

### 처리 순서(모든 기능 공통)

```
1. 로그인 인증 확인 (Spring Security 필터 체인 — 컨트롤러 진입 전)
2. 사용자/권한/리소스 검증 (예: 회차 소유자 확인)
   → 여기서 실패하면 분당 Rate Limit 검사 자체가 호출되지 않는다
3. aiRateLimitService.checkAndRecord(userId)   ← 분당 Rate Limit 검사(가장 먼저)
   → 최근 60초 요청이 10회 이상이면 BusinessException(429)을 던지고 여기서 종료
     (하루 사용량 미차감, OpenAI 미호출)
4. aiUsageService.checkAndIncrement(userId, featureType)  ← 하루 사용량 제한 검사
   → 초과 시 BusinessException(429), 역시 OpenAI 미호출
5. openAiService.generateText(...)  ← 두 검사를 모두 통과한 요청만 OpenAI를 호출한다
```

분당 Rate Limit이 하루 사용량 제한보다 먼저 검사되므로, 분당 제한에 걸린 요청은 하루 사용 횟수를
전혀 소모하지 않는다.

---

## 3. 환경변수

| 환경변수 | 기본값 | 의미 |
|---|---|---|
| `AI_RATE_LIMIT_MAX_REQUESTS` | 10 | 윈도우 내 최대 허용 요청 수 |
| `AI_RATE_LIMIT_WINDOW_SECONDS` | 60 | Sliding Window 길이(초) |

기본값을 그대로 쓸 계획이면 `.env`에 아무것도 추가하지 않아도 된다.

---

## 4. 제한 초과 응답 예시

`AI_RATE_LIMIT_EXCEEDED` (429)
```json
{
  "success": false,
  "code": "AI_RATE_LIMIT_EXCEEDED",
  "message": "AI 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
}
```

프론트엔드는 이 `message`를 그대로 사용자에게 보여준다(카운트다운 등 추가 UI는 없음). 5개 AI 기능
호출부(`EpisodeDetailPage`, `AiChatPanel`)는 이미 모든 API 오류를 `err.message`로 그대로 표시하는
공통 catch 처리를 쓰고 있어서, 이 오류 메시지를 위한 프론트엔드 코드 변경은 필요 없었다(하루 사용량
제한 초과 메시지와 동일한 경로로 이미 처리됨).

---

## 5. 동시성 제어

**선택한 방식: 비관적 락(`SELECT ... FOR UPDATE`) + `REQUIRES_NEW`**

하루 사용량 제한(`AiUsageService`)은 "제한 이내면 원자적으로 +1"이라는 고정 카운터 증가라서 조건부
`UPDATE ... WHERE count < limit` 한 줄로 원자성을 보장할 수 있었다. 하지만 분당 Rate Limit은 **최근
60초 동안의 개수(COUNT)** 를 매번 다시 계산해야 하는 Sliding Window라서, "조회(COUNT)"와 "기록(INSERT)"
이 필연적으로 두 단계로 분리된다 — 이 둘 사이에 다른 요청이 끼어들면 동시에 여러 요청이 같은 카운트를
보고 모두 통과해버리는 경쟁 상태(TOCTOU)가 생긴다.

```java
userRepository.findByIdForUpdate(userId)   // SELECT ... FOR UPDATE — 같은 사용자 요청을 직렬화
long recentCount = aiRequestLogRepository.countByUserIdAndRequestedAtAfter(userId, windowStart);
if (recentCount >= maxRequests) throw ...;
aiRequestLogRepository.save(new AiRequestLog(userId, now));
```

- `users` 테이블의 해당 사용자 행에 `SELECT ... FOR UPDATE`로 배타적 잠금을 걸어 "조회~기록" 구간을
  직렬화한다. 사용자가 다르면 잠그는 행도 달라 서로 막지 않는다 — 별도의 잠금 전용 테이블을 새로
  만들지 않고 이미 존재하는(그리고 이 시점에 반드시 존재하는) `users` 행을 재사용했다.
- `checkAndRecord(userId)`는 `AiUsageService.checkAndIncrement`와 동일하게
  `@Transactional(propagation = REQUIRES_NEW)`로 별도 트랜잭션을 쓴다. 잠금을 오래 들고 있으면 같은
  사용자의 동시 요청이 OpenAI 응답 대기 시간만큼 줄줄이 대기하게 되므로, 이 메서드가 끝나자마자
  (=OpenAI 호출 전에) 즉시 커밋되어 잠금이 곧바로 풀리도록 분리했다.
- 트레이드오프: 같은 사용자에 대한 다른 종류의 쓰기(예: 로그인 시 `refreshToken` 갱신)가 이 잠금이
  풀릴 때까지 아주 잠깐(수 ms) 대기할 수 있다. 트랜잭션이 즉시 커밋되는 짧은 구간이라 실질적인 영향은
  미미하다고 판단했다.
- 동시성 통합 테스트(`AiRateLimitServiceIntegrationTest`)에서 같은 사용자에 대해 한도(10)보다 많은
  (30) 스레드가 동시에 `checkAndRecord`를 호출해도 성공은 정확히 10번만 일어나는 것을 확인했다.

---

## 6. 오래된 로그 자동 정리

별도 배치/스케줄러를 두지 않고, **`checkAndRecord` 호출 시마다 함께 정리**하는 방식을 선택했다.

```java
aiRequestLogRepository.deleteByRequestedAtBefore(windowStart); // windowStart = now - 60초
```

- 윈도우(최근 60초) 밖으로 나간 로그는 어떤 사용자의 어떤 판단에도 더 이상 쓰이지 않으므로, 매 요청마다
  이번 사용자와 무관하게 전체 오래된 로그를 함께 삭제한다.
- 테이블 크기는 "활성 사용자 수 x 사용자당 최대 요청 수(10)" 규모로 자연스럽게 유지되고, 트래픽이 없는
  시간대에는 다음 요청이 들어올 때 한 번에 정리된다.
- 스케줄러(`@Scheduled`)를 별도로 두지 않은 이유: 이미 매 요청이 "정리가 필요한 시점"이라는 신호이기도
  하고, 삭제 대상은 인덱스(`requested_at`)를 탄 범위 삭제라 비용이 낮아 매번 실행해도 무리가 없다.
  트래픽이 매우 커지면 별도 배치로 전환할 수 있지만, 현재 규모에서는 이 방식이 가장 단순하다.

---

## 7. Flyway 마이그레이션

- `V8__add_ai_request_logs.sql` — `ai_request_logs` 테이블 신규 생성(`id`, `user_id`, `requested_at`
  + 인덱스 2개 + `users` FK). V1~V7과 동일하게 `information_schema`로 존재 여부를 먼저 확인한 뒤
  없을 때만 DDL을 동적으로 실행한다(운영 DB에 이미 반영되어 있어도 안전, 재실행해도 안전).

```sql
CREATE TABLE ai_request_logs (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    user_id      BIGINT      NOT NULL,
    requested_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_ai_request_logs_user_requested (user_id, requested_at),
    KEY idx_ai_request_logs_requested_at (requested_at),
    CONSTRAINT fk_ai_request_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

---

## 8. 테스트

### 백엔드

`./gradlew test`로 전체 실행(회귀 포함 105개 테스트, 전부 통과).

- `AiRateLimitServiceTest` — Mockito 단위 테스트. 최근 요청 수에 따른 분기(허용/차단), 차단 시
  기록되지 않는지, 정리 쿼리에 넘기는 cutoff 시각 계산이 맞는지.
- `AiRateLimitServiceIntegrationTest` — `@DataJpaTest`(H2)로 실제 SQL 동작을 검증.
  - 최근 60초 요청 9회 → 10번째 성공
  - 최근 60초 요청 10회 → 11번째 429, 로그는 여전히 10건(차단된 요청은 기록되지 않음)
  - 61초 경과 후 오래된 10건이 윈도우 밖으로 나가면 다시 요청 가능, 정리도 함께 확인
  - 챗봇/요약/세계관 등 기능을 섞어서(가정) 호출해도 합계 10회를 넘으면 차단(기능 구분 없음을 확인)
  - 서로 다른 사용자의 요청은 섞이지 않음
  - **동시성**: 같은 사용자에게 한도(10)보다 많은(30) 스레드가 동시에 요청해도 성공은 정확히 10번만
    발생 — 실제 Spring이 관리하는(`@Transactional`이 진짜 적용되는) 빈으로 검증.
    (이 테스트는 스레드마다 별도 트랜잭션/커넥션을 쓰므로, 미리 저장해 둔 사용자 행이 다른 커넥션에서도
    보이도록 `TestTransaction.flagForCommit()` + `TestTransaction.end()`로 강제 커밋한 뒤 스레드를
    띄운다.)
- `ChatServiceTest`, `ConflictDetectionServiceTest` — 기존 파일에 `AiRateLimitService` 모킹을
  추가하고, "분당 Rate Limit → 하루 사용량 → OpenAI 호출" 순서로 호출되는지(`InOrder`), 분당
  Rate Limit에서 막히면 하루 사용량 차감과 OpenAI 호출이 둘 다 전혀 일어나지 않는지를 검증하는
  테스트를 추가했다(요구사항의 "4. OpenAI 미호출", "5. 하루 사용량 미차감" 항목).

### 실행 방법

```bash
./gradlew test
./gradlew test --tests "org.example.domain.airatelimit.*"
```

### 프론트엔드

`npx tsc -b`, `npx eslint .`, `npx vite build --mode production`으로 확인했다(이 기능은 백엔드
오류 메시지가 기존 공통 catch 경로로 그대로 노출되는 구조라 프론트엔드 코드 변경이 없었다).

---

## 9. EC2 운영 환경에 추가할 설정

기본값(10회/60초)을 그대로 쓸 계획이면 아무것도 추가하지 않아도 된다. 값을 바꾸려는 경우에만 `.env`에
추가한다.

```env
AI_RATE_LIMIT_MAX_REQUESTS=10
AI_RATE_LIMIT_WINDOW_SECONDS=60
```

`docker-compose.yml`의 `app` 서비스는 이미 `env_file: - .env`로 `.env` 전체를 컨테이너에 전달하므로
추가 수정은 필요 없다. 새 테이블(`ai_request_logs`)은 Flyway가 애플리케이션 기동 시 자동으로
생성한다.

---

## 관련 문서

- [AI_USAGE_LIMIT.md](./AI_USAGE_LIMIT.md) — 하루 사용량 제한(기능별). 분당 Rate Limit은 이보다 먼저
  검사된다.
