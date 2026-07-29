# AI 하루 사용량 제한 가이드

## 목적

OpenAI API 호출은 실제 비용이 발생하므로, 사용자별로 하루 이용 가능 횟수를 제한하고 남은 횟수를
UI에 표시한다. **기능별 제한만 적용하며, 전체 합계 제한은 두지 않는다**(최초 설계에는 전체 100회
합계 제한도 있었으나, 운영 정책상 불필요하다고 판단해 제거했다 — 아래 "변경 이력" 참고).

---

## 1. 일일 제한 정책

| 기능 | 기본 제한(하루) |
|---|---|
| 회차 요약(`EPISODE_SUMMARY`) | 20회 |
| 설정 충돌 감지(`CONFLICT_DETECTION`) | 15회 |
| 등장인물 추출(`CHARACTER_EXTRACTION`) | 15회 |
| 세계관 추출(`WORLDVIEW_EXTRACTION`) | 15회 |
| AI 챗봇(`AI_CHAT`) | 20회 |

기능 유형은 `org.example.domain.aiusage.enums.AiFeatureType`(백엔드) 하나로 중앙 관리하며, 문자열을
여러 곳에 직접 적지 않는다. 제한값도 `AiUsageLimitProperties` 한 곳에서만 관리한다.

각 기능은 서로 완전히 독립적으로 카운트되고 제한된다 — 다른 기능을 많이 썼다고 해서 어떤 기능의
남은 횟수가 줄어들지 않는다. 예: AI 챗봇을 20회 사용하면 챗봇은 더 이상 사용할 수 없지만, 회차
요약·설정 충돌 감지 등 다른 기능은 각자의 한도만큼 그대로 사용할 수 있다.

`GET /api/ai/usage/daily` 응답의 각 기능 `remaining`은 그 기능 자체의 남은 횟수다(다른 기능의
사용량과 무관). 프론트엔드는 이 값을 그대로 표시하면 되고, 별도로 다시 계산하지 않는다.

---

## 2. Asia/Seoul 기준 매일 00:00 초기화 규칙

서버/DB/Docker 컨테이너의 기본 시간대가 UTC여도, 초기화 기준은 항상 **Asia/Seoul의 날짜 경계**를
따른다.

- 별도 스케줄러가 매일 자정에 전체 사용자 행을 일괄 `UPDATE`하는 방식은 사용하지 않는다.
- 대신 사용량 데이터에 **사용 기준일(`usage_date`)**을 저장하고, `(user_id, usage_date)`에 유니크
  제약을 건다. 사용자가 AI 기능을 호출하거나 사용량을 조회할 때마다 `LocalDate.now(clock)`(Asia/Seoul
  기준)으로 오늘 날짜의 행을 찾고, 없으면 그 순간 0으로 새로 만든다.
- 즉 "초기화"는 기존 행을 0으로 되돌리는 것이 아니라, 날짜가 바뀌면 **자연스럽게 새 행이 생성**되는
  방식으로 구현되어 있다. 지난 날짜의 행은 삭제되지 않고 이력으로 남는다.
- 시간대는 `Clock` 빈(`org.example.global.config.ClockConfig`, `Clock.system(ZoneId.of("Asia/Seoul"))`)을
  통해서만 얻는다. 서비스 코드에서 `LocalDate.now()`를 직접 호출하지 않으므로, 테스트에서
  `Clock.fixed(...)`로 교체해 날짜 경계 동작을 실제 시각에 의존하지 않고 검증할 수 있다.

---

## 3. 백엔드 구조

### 도메인 (`org.example.domain.aiusage`)

| 파일 | 역할 |
|---|---|
| `enums/AiFeatureType.java` | 5개 AI 기능 유형 + 한글 라벨(오류 메시지에 사용) |
| `entity/AiDailyUsage.java` | 사용자별·날짜별 카운터 엔티티. `user`와의 JPA 연관관계 없이 `userId`(순수 컬럼)만 저장 — 참조 무결성은 DB FK로 보장 |
| `repository/AiDailyUsageRepository.java` | 조회 1개 + 기능별 조건부 UPDATE 5개(`incrementXxxIfAllowed`) |
| `config/AiUsageLimitProperties.java` | 제한값(`app.ai.usage.daily-limit.*`) 바인딩 |
| `service/AiUsageService.java` | 검사·증가·조회를 담당하는 단일 창구. `checkAndIncrement(userId, featureType)` / `getDailyUsage(email)` |
| `service/AiDailyUsageRowInitializer.java` | 오늘 행이 없으면 생성하는 전용 컴포넌트(왜 분리했는지는 아래 "동시성 제어" 참고) |
| `dto/AiUsageDetailDto.java`, `dto/AiDailyUsageResponseDto.java` | 조회 응답 DTO |
| `controller/AiUsageController.java` | `GET /api/ai/usage/daily` |

### 각 AI 서비스와의 연동

`EpisodeSummaryService`, `ConflictDetectionService`, `CharacterExtractionService`,
`WorldSettingExtractionService`, `ChatService`는 각각 `AiUsageService`를 주입받아, **OpenAI 호출 직전
한 줄**(`aiUsageService.checkAndIncrement(user.getId(), AiFeatureType.XXX)`)만 추가했다. 제한 검사·
원자적 증가·예외 분기 로직은 전부 `AiUsageService` 하나에 모여 있어 기능별 서비스마다 같은 코드를
복사하지 않는다.

호출 순서(모든 기능 공통, 2026-07-29 분당 Rate Limit 추가 이후):
```
1. 사용자/권한/리소스 검증 (예: 회차 소유자 확인)
   → 여기서 실패하면 아래 검사들이 전혀 호출되지 않는다(분당/하루 횟수 모두 미차감)
2. aiRateLimitService.checkAndRecord(userId)  ← 분당 Rate Limit(최근 60초, 전체 요청 합계) — 이 문서가
   다루는 하루 사용량보다 먼저 검사한다. 초과 시 BusinessException(429)으로 여기서 종료.
   자세한 내용은 AI_RATE_LIMIT.md 참고.
3. checkAndIncrement(userId, featureType)  ← OpenAI 호출 직전
   → 제한 초과 시 BusinessException(429)을 던지고 여기서 종료(횟수 미차감)
4. openAiService.generateText(...)  ← 사용량은 이미 커밋된 뒤이므로,
   여기서 OpenAI 오류/타임아웃/파싱 오류가 나도 방금 차감된 횟수는 복구하지 않는다.
```

---

## 4. 환경변수

Spring 설정 파일(`src/main/resources/application.yml`)의 `app.ai.usage.daily-limit.*`가 아래
환경변수를 읽고, 값이 없으면 표에 적힌 기본값을 사용한다.

| 환경변수 | 기본값 | 대응 기능 |
|---|---|---|
| `AI_DAILY_SUMMARY_LIMIT` | 20 | 회차 요약 |
| `AI_DAILY_CONFLICT_LIMIT` | 15 | 설정 충돌 감지 |
| `AI_DAILY_CHARACTER_LIMIT` | 15 | 등장인물 추출 |
| `AI_DAILY_WORLDVIEW_LIMIT` | 15 | 세계관 추출 |
| `AI_DAILY_CHAT_LIMIT` | 20 | AI 챗봇 |

이 값들은 `application.yml`(운영/로컬 공통 베이스 설정)에 있으므로 로컬·운영 모두 동일하게 적용된다.
운영에서 값을 바꾸려면 **코드 수정 없이** EC2 `.env`에 해당 환경변수만 추가/변경하고 컨테이너를
재생성하면 된다(아래 "EC2 운영 환경" 참고).

`AI_DAILY_TOTAL_LIMIT`는 더 이상 사용하지 않는다(아래 "변경 이력" 참고). 운영 `.env`에 이미
추가했다면 지워도 되고, 남겨둬도 애플리케이션이 그냥 무시하므로 무해하다.

---

## 5. 사용량 조회 API

```
GET /api/ai/usage/daily
Authorization: Bearer {accessToken}
```

이 조회 자체는 사용 횟수에 포함되지 않는다(읽기 전용, `@Transactional(readOnly = true)`).

**Response (200)**
```json
{
  "success": true,
  "code": "OK",
  "message": "AI 사용량 조회 성공",
  "data": {
    "usageDate": "2026-07-29",
    "timezone": "Asia/Seoul",
    "nextResetAt": "2026-07-30T00:00:00+09:00",
    "summary":   { "used": 1, "remaining": 19, "limit": 20 },
    "conflict":  { "used": 1, "remaining": 14, "limit": 15 },
    "character": { "used": 1, "remaining": 14, "limit": 15 },
    "worldview": { "used": 1, "remaining": 14, "limit": 15 },
    "chat":      { "used": 2, "remaining": 18, "limit": 20 }
  }
}
```

프론트엔드 연동 상세(공통 훅/컴포넌트, 표시 위치)는 `FRONTEND_API.md`의
"AI 사용량(AiUsage) API" 섹션을 참고.

---

## 6. 제한 초과 응답 예시

기존 `ErrorCode`/`BusinessException`/`GlobalExceptionHandler` 구조를 그대로 활용한다. 이미
`EMAIL_CODE_RESEND_TOO_SOON`이 같은 용도로 `429 Too Many Requests`를 쓰고 있어 동일한 관례를
따랐다.

**기능 제한 초과** — `AI_DAILY_FEATURE_LIMIT_EXCEEDED` (429)
```json
{
  "success": false,
  "code": "AI_DAILY_FEATURE_LIMIT_EXCEEDED",
  "message": "오늘 사용할 수 있는 회차 요약 횟수를 모두 사용했습니다. AI 도구 이용권은 매일 00:00에 충전됩니다."
}
```
메시지의 기능명(`회차 요약` 등)은 `AiFeatureType.getLabel()`에서 가져오므로 기능마다 하드코딩하지 않는다.

프론트엔드는 이 `message`를 그대로 사용자에게 보여준다.

---

## 7. 동시성 제어

**선택한 방식: 조건부 UPDATE (원자적 증가)**

```sql
UPDATE ai_daily_usages
SET summary_count = summary_count + 1
WHERE user_id = :userId AND usage_date = :usageDate AND summary_count < :featureLimit
```

- 값을 먼저 조회한 뒤 애플리케이션 메모리에서 더하고 다시 저장하는 방식(동시 요청에 취약)은
  사용하지 않는다. 위 UPDATE 문 하나로 "제한 이내인지 확인 + 증가"를 원자적으로 처리한다.
- 갱신된 행 수가 1이면 성공, 0이면 실패(제한 초과 또는 아직 행이 없음)로 판단한다. MySQL InnoDB의
  행 잠금 덕분에 같은 사용자의 동시 요청은 이 UPDATE 문에서 자연스럽게 직렬화되어, 여러 서버
  인스턴스로 확장되어도(로컬 메모리가 아니라 DB가 기준이므로) 제한을 초과할 수 없다.
- **왜 비관적 락(`SELECT ... FOR UPDATE`)이나 낙관적 락(`@Version`)이 아닌 조건부 UPDATE를
  선택했는가**: "조회 후 조건에 따라 갱신"을 별도 락 없이 한 왕복(round-trip)의 UPDATE 문으로 끝낼 수
  있어 가장 단순하고, 재시도 로직(낙관적 락의 경우 필요)도 필요 없다. 조회를 먼저 하고 잠그는 비관적
  락 방식보다 락 보유 시간이 짧다.
- `checkAndIncrement(userId, featureType)`는 `@Transactional(propagation = REQUIRES_NEW)`로 별도
  트랜잭션을 사용한다. 이유는 두 가지다.
  1. 호출하는 AI 서비스 메서드가 `readOnly` 트랜잭션(`CharacterExtractionService`,
     `WorldSettingExtractionService`, `ChatService`)일 수 있다.
  2. "OpenAI 요청 전송 이후 실패해도 이미 차감된 사용 횟수는 복구하지 않는다"는 요구사항을 만족하려면,
     이 증가가 호출부 트랜잭션의 롤백 여부와 무관하게 **즉시 커밋**되어야 한다.
- **행 생성과 증가를 별도 빈으로 분리한 이유**: 오늘 첫 호출 시 행이 없으면 새로 만드는데,
  동시에 여러 요청이 처음 진입하면 `(user_id, usage_date)` 유니크 제약으로 하나만 성공하고 나머지는
  `DataIntegrityViolationException`을 받는다. Hibernate는 flush 중 이런 예외가 나면 **자바 코드에서
  잡아도** 그 트랜잭션을 내부적으로 rollback-only로 표시해버려, 같은 트랜잭션에서 계속 진행하면
  커밋 시점에 `UnexpectedRollbackException`이 난다. 그래서 행 생성은 `AiDailyUsageRowInitializer`라는
  별도 빈의 `REQUIRES_NEW` 트랜잭션으로 분리했다 — 실패해도 그 트랜잭션만 깨끗하게 롤백·종료되고,
  `AiUsageService.checkAndIncrement()`가 이어서 쓰는 트랜잭션(세션)은 전혀 영향을 받지 않는다. 이
  문제는 실제로 동시성 통합 테스트(`AiUsageServiceIntegrationTest`)를 작성하는 과정에서 발견되어
  수정했다.

---

## 8. Flyway 마이그레이션

- `V6__add_ai_daily_usage.sql` — `ai_daily_usages` 테이블 신규 생성(전체 합계 컬럼 `total_count`
  포함한 최초 버전).
- `V7__remove_ai_daily_usage_total_count.sql` — 전체 합계 제한 정책을 폐지하면서 `total_count`
  컬럼을 제거. 두 마이그레이션 모두 V1~V5와 동일하게 `information_schema`로 존재 여부를 먼저
  확인한 뒤 필요할 때만 DDL을 동적으로 실행한다(운영 DB에 이미 반영되어 있어도 안전, 재실행해도
  안전).

```sql
-- V7__remove_ai_daily_usage_total_count.sql
ALTER TABLE ai_daily_usages DROP COLUMN total_count;
```

- 운영은 `spring.jpa.hibernate.ddl-auto=validate`를 그대로 유지한다. 스키마 변경은 이 마이그레이션
  파일들로만 이루어지며, 엔티티 자동 반영에 의존하지 않는다.
- 로컬 개발 환경(`mydb`, MySQL 9.6)에 V6·V7을 실제로 순서대로 적용해 확인했다 — Flyway가 schema
  `mydb`를 version 5 → 6 → 7로 마이그레이션했고, `DESCRIBE`/`SHOW CREATE TABLE`로 `total_count`
  컬럼이 제거되고 나머지 컬럼·유니크 제약·FK는 그대로인 것을 직접 확인했다.

---

## 9. 테스트

### 백엔드

`./gradlew test`로 전체 실행(회귀 포함 94개 테스트, 전부 통과).

- `AiUsageServiceTest` — Mockito 단위 테스트. 리포지토리 반환값에 따른 분기 로직(제한 초과 예외),
  `remaining` 계산(기능 자체의 잔여량), 조회가 증가 메서드를 호출하지 않는지 등.
- `AiDailyUsageRowInitializerTest` — 행 생성/중복키 처리를 별도로 검증.
- `AiUsageServiceIntegrationTest` — `@DataJpaTest`(H2 인메모리 DB)로 실제 SQL 동작을 검증.
  - 회차 요약 20회 후 21번째 차단, 설정 충돌 감지·등장인물 추출·세계관 추출 15회 후 16번째 차단,
    AI 챗봇 20회 후 21번째 차단(각 기능이 서로 독립적으로 제한되는지 확인)
  - Asia/Seoul 기준 날짜가 바뀌면 다시 사용 가능
  - 서로 다른 사용자의 사용량이 섞이지 않음
  - **동시성**: 50개 스레드가 동시에 같은 기능을 호출해도 성공은 정확히 한도(기본 20)만큼만 발생하고,
    최종 DB 카운트가 한도와 정확히 일치 — Spring이 실제로 관리하는(`@Transactional`이 진짜 적용되는)
    빈으로 검증한다.
- `ChatServiceTest`, `ConflictDetectionServiceTest` — 기존 파일에 `AiUsageService` 모킹을 추가하고,
  "권한 검증 실패 시 `checkAndIncrement`가 호출되지 않는지"(OpenAI 호출 이전 실패는 횟수 미포함),
  "OpenAI 호출 이후 오류가 나도 `checkAndIncrement`는 정확히 한 번만 호출되고 별도 복구 호출이
  없는지"를 검증하는 테스트를 추가했다.

시간 관련 테스트는 전부 `Clock.fixed(...)`를 주입해 실행 시각과 무관하게 안정적으로 동작한다.

동시성 테스트만 H2가 필요해 `build.gradle`에 `testRuntimeOnly 'com.h2database:h2'`를 추가했고(운영
런타임에는 영향 없음), `src/test/resources/application.yml`에서 이 경우에 한해 Flyway를 끄고
Hibernate가 엔티티 기준으로 스키마를 즉석 생성하도록 했다(V1~V7은 MySQL 전용 동적 SQL을 써서 H2에서
그대로 실행할 수 없기 때문).

### 실행 방법

```bash
./gradlew test
./gradlew test --tests "org.example.domain.aiusage.*"
```

### 프론트엔드

이 프로젝트는 별도 프론트엔드 테스트 러너(Jest/Vitest 등)가 구성되어 있지 않다(`package.json`에
`test` 스크립트 없음). 대신 아래로 검증했다.

- `npx tsc -b` — 타입 검사
- `npx eslint .` — 린트(수정한 파일 기준 새 에러 없음)
- `npx vite build --mode production` — 프로덕션 빌드

---

## 10. 프론트 표시 위치

| 화면 | 위치 |
|---|---|
| 회차 상세 페이지 | AI 도구 박스 내 4개 카드(요약/충돌감지/인물추출/세계관추출) 각각의 헤더 바로 아래에 "오늘 남은 횟수 N / M회" 표시, 박스 하단에 "AI 도구 이용권은 매일 00:00에 충전됩니다." |
| 작품 상세 / 회차 작성 / 회차 수정 페이지 | 공통 `AiChatPanel` 헤더 아래에 챗봇 잔여 횟수 표시, 입력 영역 아래에 동일한 충전 안내 문구 |

남은 횟수가 0이면 해당 실행 버튼(또는 챗봇 입력창/전송 버튼)이 비활성화되고, "오늘 이용 가능한
횟수를 모두 사용했습니다."(챗봇은 "AI 채팅 횟수를 모두 사용했습니다.") 안내가 표시된다.

사용량 조회가 실패하면(`error` 존재) 무제한으로 간주하지 않고 실행 버튼을 비활성화하며, "사용 가능
횟수를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."를 표시한다. 프론트 제한은 사용자 편의를 위한
것이고, 실제 보안·비용 보호는 백엔드(`AiUsageService`)가 강제한다.

---

## 11. EC2 운영 환경에 추가할 설정

### `.env`에 추가 (필요한 경우에만)

기본값을 그대로 쓸 계획이면 아무것도 추가하지 않아도 된다(코드 기본값이 요구사항의 기본 정책과
동일). 제한을 조정하려는 경우에만 필요한 항목을 추가한다.

```env
AI_DAILY_SUMMARY_LIMIT=20
AI_DAILY_CONFLICT_LIMIT=15
AI_DAILY_CHARACTER_LIMIT=15
AI_DAILY_WORLDVIEW_LIMIT=15
AI_DAILY_CHAT_LIMIT=20
```

### Docker Compose / 배포

- `docker-compose.yml`의 `app` 서비스는 이미 `env_file: - .env`로 `.env` 전체를 컨테이너에 전달하고
  있으므로, 위 변수를 추가해도 `docker-compose.yml` 자체는 수정할 필요가 없다.
- 값을 바꾼 뒤에는 컨테이너를 **재생성**해야 반영된다(`docker compose restart`는 기존 컨테이너에
  주입된 환경변수를 다시 읽지 않는다).
  ```bash
  docker compose up -d app
  # 확실히 하려면
  docker compose up -d --force-recreate app
  ```
- 새 테이블(`ai_daily_usages`)과 컬럼 변경은 Flyway가 애플리케이션 기동 시 자동으로 처리한다 —
  별도의 수동 DB 작업은 필요 없다. 다만 운영 배포 전에 아래를 확인할 것을 권장한다.
  - 배포 파이프라인(GitHub Actions 등)이 `./gradlew build`/테스트를 통과하는지
  - 운영 `SPRING_PROFILES_ACTIVE=prod`가 설정되어 `ddl-auto=validate` + Flyway 경로를 타는지(로컬
    `update` 경로로 우회되지 않는지)
  - 첫 배포 직후 애플리케이션 로그에서 `Migrating schema ... to version "7 - remove ai daily usage
    total count"`가 보이는지(정상 적용 확인)

---

## 12. 향후 FREE / PRO / PREMIUM 플랜별 제한 확장 방법

`User` 엔티티에는 이미 향후 유료 플랜 확장을 위한 `Plan`(`FREE`/`PREMIUM`) 필드가 있다. 지금은 모든
사용자에게 동일한 제한을 적용하지만, 플랜별로 다른 제한을 주고 싶다면 아래 방향을 검토한다(이번
작업 범위에는 포함하지 않았다).

1. `AiUsageLimitProperties`를 플랜별로 나눈다 — 예: `app.ai.usage.daily-limit.free.*` /
   `app.ai.usage.daily-limit.premium.*` 두 그룹으로 확장하고, `AiUsageService`가
   `user.getPlan()`에 따라 어느 그룹을 쓸지 선택하게 한다. 기존 프로퍼티 바인딩 구조를 그대로
   재사용할 수 있어 가장 적은 변경으로 확장 가능하다.
2. 또는 플랜별 제한을 DB 테이블(`plan_limits` 등)로 관리해, 운영 중 배포 없이(환경변수 재배포 없이)
   관리자가 값을 바꿀 수 있게 한다 — 제한 조정 빈도가 잦아지면 고려.
   2번을 선택하더라도 `AiDailyUsageRepository`의 조건부 UPDATE 쿼리 자체
   (`... AND count < :featureLimit`)는 그대로 재사용 가능하다. `featureLimit`을 어디서 읽어오는지만
   바뀐다.
3. `AiFeatureType`, `AiDailyUsage` 테이블 스키마, `checkAndIncrement`/`getDailyUsage`의 시그니처는
   플랜 도입과 무관하게 그대로 유지 가능하다 — "기능별 제한 검사"라는 책임과 "플랜별 제한값 조회"라는
   책임이 이미 분리되어 있기 때문이다(`AiUsageLimitProperties`가 후자만 담당).
4. 만약 향후 다시 "전체 합계 제한"이 필요해지면, `ai_daily_usages`에 다시 합계 컬럼을 추가하는
   Flyway 마이그레이션을 작성하고, `AiDailyUsageRepository`의 조건부 UPDATE에 `AND total_count <
   :totalLimit` 조건을, `AiUsageService`에는 "어느 제한 때문에 막혔는지" 구분하는 분기(기존 V6 설계에
   있었던 방식)를 다시 추가하면 된다. 이번 변경으로 관련 코드가 완전히 제거되었으므로 필요 시 git
   히스토리에서 이전 구현을 참고할 수 있다.

---

## 변경 이력

- **최초 설계**: 기능별 제한 + 전체 합계 100회 제한을 함께 적용(`total_count` 컬럼,
  `AI_DAILY_TOTAL_LIMIT` 환경변수, `AI_DAILY_TOTAL_LIMIT_EXCEEDED` 예외).
- **정책 변경**: 전체 합계 제한이 불필요하다고 판단되어 제거. 기능별 제한만 남기고,
  `total_count` 컬럼은 `V7__remove_ai_daily_usage_total_count.sql`로 삭제했다.
- **정책 변경**: AI 챗봇 하루 제한을 50회에서 20회로 하향 조정
  (`AiUsageLimitProperties.chat` 기본값, `AI_DAILY_CHAT_LIMIT` 기본값). 스키마/코드 구조 변경은
  없고 제한값(설정)만 바뀐 것이라 별도 Flyway 마이그레이션은 필요하지 않다.
- **기능 추가**: 이 하루 사용량 제한과는 별도로, 짧은 시간에 몰아서 요청하는 남용을 막기 위한
  **분당 Rate Limit**(사용자당 최근 60초 10회, 5개 기능 전체 합계 기준)을 추가했다. 이 하루 사용량
  검사보다 먼저 실행되며, 자세한 내용은 [AI_RATE_LIMIT.md](./AI_RATE_LIMIT.md) 참고.

이 문서는 위 변경들을 모두 반영한 최종 정책을 기준으로 작성되어 있다.
