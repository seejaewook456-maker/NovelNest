# Sentry 오류 수집 가이드

## 목적

프론트엔드(React)와 백엔드(Spring Boot)에서 발생하는 **운영 오류**를 안전하게 수집하기 위해
Sentry를 연동했다. NovelNest는 소설 본문·AI 응답 등 사용자 작성 콘텐츠를 다루므로, 이 문서에
정리된 개인정보/콘텐츠 비수집 정책을 반드시 지켜야 한다.

이번 연동은 **기본 오류 수집(에러 모니터링)만** 사용한다. 아래 기능은 의도적으로 적용하지 않았다.

- Source Map 업로드(`@sentry/vite-plugin`, `SENTRY_AUTH_TOKEN`/`SENTRY_ORG`/`SENTRY_PROJECT`)
- 성능 모니터링(`tracesSampleRate`, Browser Tracing integration)
- 분산 추적(`tracePropagationTargets`, `sentry-trace`/`baggage` 헤더)
- Sentry Replay — Microsoft Clarity가 이미 세션 리플레이를 담당하므로 중복 적용하지 않음
- Release 관리, GitHub 연동
- 사용자 식별 정보 등록(`Sentry.setUser()` 등은 어디서도 호출하지 않음)

## 프로젝트 분리

프론트엔드용 Sentry 프로젝트(DSN)와 백엔드용 Sentry 프로젝트(DSN)를 **반드시 별도로** 생성해서
사용한다. 하나의 DSN을 두 곳에 함께 쓰지 않는다 — 이슈가 뒤섞이는 것을 막기 위함이다.

---

## 프론트엔드

### 구조

| 파일 | 역할 |
|---|---|
| `frontend/src/lib/sentry.ts` | `initializeSentry()` / `captureApiError()` — Sentry 초기화와 API 오류 수집을 담당하는 유일한 모듈. `beforeSend`/`beforeBreadcrumb`로 전송 직전 민감정보를 제거한다 |
| `frontend/src/components/AppErrorBoundary.tsx` | `Sentry.ErrorBoundary`를 NovelNest 디자인(Warm Brown + Cream)에 맞는 fallback UI로 감싼 컴포넌트 |
| `frontend/src/main.tsx` | 진입점에서 `initializeSentry()`를 GA4/Clarity와 함께 1회 호출 |
| `frontend/src/App.tsx` | `AppErrorBoundary`로 라우터 전체를 감쌈 |
| `frontend/src/api/fetchWithAuth.ts` | 유일한 공통 API 오류 처리 지점 — 여기서만 `captureApiError()`를 호출한다 |

### 초기화

`initializeSentry()`는 다음 조건에서만 실제로 초기화한다.

- `import.meta.env.PROD`가 `true`일 것 (Vite 운영 빌드)
- `import.meta.env.VITE_SENTRY_DSN`이 설정되어 있을 것

둘 중 하나라도 아니면 아무 것도 하지 않고 조용히 반환한다 — 로컬 개발(`npm run dev`)과 `vite build --mode development`는 항상 비활성화된다. 초기화 로직 전체를 `try/catch`로 감싸, 초기화 자체가 실패해도 앱 렌더링을 막지 않는다. 모듈 내부 `initialized` 플래그로 중복 초기화를 막는다(React StrictMode로 컴포넌트가 두 번 렌더링돼도, 초기화 호출 자체는 `main.tsx` 최상위에서 한 번만 실행되므로 애초에 중복 위험이 없다).

`Sentry.init()`에는 `sendDefaultPii: false`만 명시하고 `integrations`/`tracesSampleRate`는 지정하지
않는다 — Replay·Browser Tracing은 명시적으로 추가해야만 켜지는 옵트인 기능이라, **아무것도 추가하지
않는 것 자체가 미적용을 보장**한다. 처리되지 않은 JS 예외, 처리되지 않은 Promise rejection은 SDK
기본 integration이 자동으로 수집한다.

### Error Boundary

`AppErrorBoundary`(내부적으로 `Sentry.ErrorBoundary` 사용)가 `App.tsx`에서 `<RouterProvider>`와
`<SessionExpiredModal>`을 함께 감싸, 렌더링 중 예상하지 못한 오류가 나면 흰 화면 대신 안내 화면과
"새로고침" 버튼(`window.location.reload()`)을 보여준다. fallback UI는 사용자 입력이나 작품 내용을
전혀 다루지 않는 정적 화면이다.

### API 오류 수집 기준 (`captureApiError`)

이 프로젝트는 Axios가 아니라 `fetch` 기반 공통 래퍼(`fetchWithAuth`)를 사용한다. 모든 API 함수가
이 래퍼를 거치므로, `fetchWithAuth.ts` 한 곳에서만 `captureApiError()`를 호출하도록 구현했다 —
개별 API 함수(`novelApi.ts`, `episodeApi.ts` 등 약 10개 파일)는 전혀 수정하지 않았다.

**수집하는 경우**

- `res.status >= 500`
- `fetch()` 자체가 실패한 경우(오프라인, 서버 다운, CORS 등 → `NetworkError`)
- 응답 바디가 JSON으로 파싱되지 않는 예상하지 못한 경우

**수집하지 않는 경우**

- 400/401/403/404를 포함한 모든 4xx (입력값 오류, 로그인 만료/미인증, 권한 거부, 존재하지 않는
  리소스 등 예상 가능한 비즈니스 오류)

Context에는 다음만 담는다.

- `feature` 태그 — 요청 경로에서 안전하게 유추 가능한 경우에만: `auth` / `novel` / `episode` /
  `character` / `worldsetting` / `ai_summary` / `ai_conflict` / `ai_character` / `ai_worldview` /
  `ai_chat`. 애매한 경로는 태그를 붙이지 않는다.
- HTTP 메서드, HTTP 상태 코드
- 쿼리스트링을 제거한 API 경로 (`stripQueryString()`)
- 현재 화면 경로(`window.location.pathname`, 사용자 식별 정보 없음)

인증(로그인/회원가입/이메일 인증)은 `authApi.ts`가 `fetchWithAuth`를 거치지 않는 별도의 raw
`fetch()`를 사용한다(인증 전 호출이라 Authorization 헤더가 필요 없음). 이 경로의 실패는 대부분
400(입력값 오류/중복 이메일 등 정상적인 검증 실패)이라 수집 대상에서 제외되는 경우가 많고, 여기에
개별적으로 캡처 코드를 추가하면 "모든 API 함수에 중복 코드 추가"가 되므로 넣지 않았다. `/auth/refresh`
(토큰 재발급, `fetchWithAuth` 내부에서 자체 호출)는 계측되어 있다.

### 개인정보 제거 (`beforeSend` / `beforeBreadcrumb`)

`beforeSend`가 전송 직전 마지막 방어선으로 아래를 제거한다.

- `event.request.data`, `event.request.cookies`, `event.request.query_string`
- `event.request.url`의 쿼리스트링 (`stripQueryString()`)
- `event.request.headers`의 `authorization`/`cookie` (대소문자 무관)
- `event.user.email` / `event.user.username` / `event.user.ip_address` (애초에 `Sentry.setUser()`를
  호출하지 않으므로 대부분 비어 있지만, 방어적으로 한 번 더 제거)
- breadcrumb 중 `category === 'console'` 전부 (콘솔 로그에 오류 메시지·사용자 콘텐츠가 그대로 찍힐
  수 있어 통째로 제외)
- 남은 breadcrumb(`fetch`/`xhr`/`navigation`)의 `url`/`to`/`from` 쿼리스트링 (예:
  `/oauth2/callback?token=...` 같은 리다이렉트 URL이 breadcrumb에 그대로 남는 것을 방지)

`beforeBreadcrumb`가 같은 정리를 breadcrumb 생성 시점에도 한 번 더 적용한다(이중 방어).

### 환경변수

| 변수 | 위치 | 설명 |
|---|---|---|
| `VITE_SENTRY_DSN` | Vercel Environment Variables (Production) | 프론트엔드용 Sentry DSN. `.env.development`/`.env.production`에는 커밋하지 않음 — 실제 값은 사용자가 Vercel에 직접 등록 |

Vercel 등록 후에는 **재배포가 필요**하다 — Vite는 빌드 시점에 `import.meta.env.VITE_*` 값을 번들에
굳히기 때문에, 값을 등록/변경한 뒤 재배포하지 않으면 반영되지 않는다.

---

## 백엔드

### 구조

- 의존성: `io.sentry:sentry-spring-boot-starter-jakarta:8.50.1` (Spring Boot 3.4.1 + Java 17,
  Jakarta 네임스페이스 공식 스타터)
- 설정: `application-prod.yml`에만 `sentry:` 블록 추가 (local/test에는 없음 → 운영 프로필 외에는
  자동 활성화되지 않음)
- 수동 캡처(`Sentry.captureException(e)`)를 추가한 위치 4곳 — 이유는 "중복 수집 방지" 절 참고
  - `global/exception/GlobalExceptionHandler.java` — 처리되지 않은 최상위 `Exception` 핸들러
  - `global/config/AsyncConfig.java` — `@Async` 메서드의 `AsyncUncaughtExceptionHandler`
  - `global/scheduler/VerificationCleanupScheduler.java` — 주기적 정리 작업의 각 `catch` 블록
  - `global/ai/service/OpenAiService.java` — OpenAI API 호출 실패(타임아웃/5xx/연결 오류)

### 설정 (`application-prod.yml`)

```yaml
sentry:
  dsn: ${SENTRY_DSN:}
  environment: ${SENTRY_ENVIRONMENT:production}
  send-default-pii: false
  max-request-body-size: none
```

- `dsn`이 비어 있으면(EC2 `.env`에 `SENTRY_DSN`을 아직 등록하지 않은 경우) SDK가 이벤트를 전송하지
  않는 안전한 no-op으로 동작한다 — 서버 기동 자체는 정상적으로 이루어진다.
- `send-default-pii: false`로 개인정보 기본 수집을 비활성화한다.
- `max-request-body-size: none`으로 HTTP 요청 Body를 아예 수집하지 않는다.
- `traces-sample-rate` 등 성능 모니터링 관련 프로퍼티는 추가하지 않았다(추가하지 않는 것 자체가
  비활성화를 보장한다).
- 이 블록은 `application-prod.yml`에만 있으므로 `local`/`test` 프로필에서는 로드되지 않는다 — 로컬
  설정 파일에는 실제 DSN을 넣지 않았고, 넣을 수도 없는 구조다.

기존 `ddl-auto=validate`, Flyway, DB 연결, JWT, OAuth2, CORS, 이메일, OpenAI 설정은 전혀
건드리지 않았다. 엔티티/테이블/컬럼 변경이 없으므로 Flyway 마이그레이션 파일도 추가하지 않았다.

### 백엔드 오류 수집 기준

**수집 대상**

- 예상하지 못한 500 오류, 처리되지 않은 `RuntimeException`/`NullPointerException`
- DB 연결/쿼리 실행 중 예상하지 못한 오류 (전용 핸들러가 없어 catch-all로 흘러들어옴)
- OpenAI API 타임아웃/연결 오류/5xx
- `@Async` 작업, 주기적 정리 작업(스케줄러)에서 발생한 예상하지 못한 예외

**수집 제외 대상**

- `BusinessException`(로그인 실패, 이메일 중복, 인증번호 불일치/만료, 존재하지 않는 작품/회차 등
  `ErrorCode` 기반 정상 비즈니스 예외)
- 요청 검증 실패(`MethodArgumentNotValidException`/`BindException`), 잘못된 JSON 형식
- 인증/인가 관련 401/403(`AuthenticationException`/`AccessDeniedException`)
- 존재하지 않는 URL(404)
- 즉, `GlobalExceptionHandler`에서 전용 `@ExceptionHandler`가 이미 처리하는 모든 유형

### 중복 수집 방지

`GlobalExceptionHandler`는 `@ExceptionHandler(Exception.class)`로 **모든** 예외를 잡는
catch-all을 이미 갖고 있다. Spring은 `@RestControllerAdvice`의 `ExceptionHandlerExceptionResolver`가
예외를 한 번 해결(resolve)하면 그 뒤의 리졸버 체인(Sentry의 자동 `HandlerExceptionResolver` 포함)을
더 이상 타지 않는다 — 즉 **이 구조에서는 Sentry의 자동 예외 수집이 컨트롤러 계층에서는 사실상 전혀
작동하지 않는다.** 그래서:

- catch-all(`handleException`)에서만 `Sentry.captureException(e)`를 호출한다. `BusinessException`
  등 더 구체적인 타입은 그 위의 전용 핸들러가 먼저 처리해 catch-all에 도달하지 않으므로, 같은 예외가
  두 번 전송될 여지가 없다.
- `@Async`/스케줄러/OpenAI 호출부는 애초에 컨트롤러 계층을 거치지 않고 자체 `try/catch`로 예외를
  삼키는 구조라(그렇지 않으면 로그에도 안 남고 조용히 사라짐), 각 지점에서 **한 번씩만** 수동
  캡처했다. 이 네 곳 각각은 서로 다른 예외 발생 지점이라 중복 가능성이 없다.
- 로그(`log.error`/`log.warn`)와 Sentry가 별도로 중복 전송되는 경로는 없다 — 이 프로젝트는
  `logback-spring.xml`/`SentryAppender`를 사용하지 않으므로, 일반 로그 호출이 자동으로 Sentry
  이벤트가 되는 일은 없다(수동으로 `Sentry.captureException()`을 호출한 곳만 전송됨).

### 개인정보 제거

`sentry.max-request-body-size: none` + `sentry.send-default-pii: false` 조합으로 요청 Body,
쿠키, 인증 헤더, 사용자 IP 등이 기본적으로 수집되지 않는다. 수동으로 캡처하는 4곳 모두 예외
객체(`e`/`throwable`)만 전달하고, 이메일/닉네임/인증번호/토큰/작품·회차 내용/AI 요청·응답 등을
Sentry 컨텍스트에 추가하는 코드는 어디에도 없다. 기존 로그 메시지도 함께 점검했다 —
`EmailSendEventListener`는 이미 이메일을 마스킹(`***@domain`)해서만 로그로 남기고,
`AsyncConfig`의 예외 로그는 메서드 이름/시그니처만 남긴다(인자 값 자체는 로그에 남기지 않음). 이번
작업에서 새로 추가한 로그는 없다.

### 환경변수

| 변수 | 위치 | 설명 |
|---|---|---|
| `SENTRY_DSN` | EC2 `.env` | 백엔드용 Sentry DSN. 프론트엔드 DSN과 다른 프로젝트를 사용해야 한다 |
| `SENTRY_ENVIRONMENT` | EC2 `.env` | 기본값 `production` (미설정 시 자동 적용) |

```env
SENTRY_DSN=https://백엔드용-DSN
SENTRY_ENVIRONMENT=production
```

### Docker Compose

`docker-compose.yml`의 `app` 서비스는 이미 `env_file: - .env`로 `.env` 파일 전체를 컨테이너에
전달하고 있다. 따라서 `SENTRY_DSN`/`SENTRY_ENVIRONMENT`를 위해 `docker-compose.yml`을 수정할
필요가 **없다** — EC2의 `.env`에 두 값을 추가하기만 하면 된다.

**주의:** `.env` 파일을 수정한 뒤 `docker compose restart app`만 실행하면 안 된다. `restart`는
이미 떠 있는 컨테이너를 그대로 재시작할 뿐, 컨테이너에 새로 주입될 환경변수를 다시 읽지 않는다.
새 환경변수를 반영하려면 컨테이너를 **다시 생성**해야 한다.

```bash
# .env 수정 후
docker compose up -d app
# 확실히 하려면
docker compose up -d --force-recreate app
```

---

## 로컬에서는 비활성화되는 구조 (요약)

| 구분 | 로컬 | 운영 |
|---|---|---|
| 프론트엔드 | `import.meta.env.PROD`가 false거나 `VITE_SENTRY_DSN` 없음 → 초기화 안 함 | Vercel에 `VITE_SENTRY_DSN` 등록 시 활성화 |
| 백엔드 | `application-prod.yml`이 로드되지 않음(프로필 자체가 다름) → `sentry:` 설정 없음 | `SPRING_PROFILES_ACTIVE=prod` + `SENTRY_DSN` 설정 시 활성화 |

## 운영 배포 후 확인 방법

1. **프론트엔드**: Vercel에 `VITE_SENTRY_DSN` 등록 → 재배포 → `https://www.novelnestia.com`에서
   개발자 도구 Network 탭에 `sentry.io`(또는 커스텀 ingest 도메인)로의 요청이 발생하는지 확인.
2. **백엔드**: EC2 `.env`에 `SENTRY_DSN`/`SENTRY_ENVIRONMENT` 추가 → `docker compose up -d app`으로
   컨테이너 재생성 → 서버가 정상 기동하는지 로그 확인.
3. [Sentry Issues](https://sentry.io) 대시보드에서 프론트엔드/백엔드 각 프로젝트에 새 이슈가
   올라오는지 확인. 이슈 상세에서 요청 Body/쿠키/Authorization 헤더/작품 내용 등이 보이지 않는지
   직접 눈으로 확인하는 것을 권장한다.

## 테스트 오류 코드 관련 주의사항

연동 확인용으로 아래와 같은 테스트 오류를 사용할 수 있다.

```ts
// 프론트엔드
throw new Error("Sentry frontend integration test");
```

```java
// 백엔드
throw new IllegalStateException("Sentry backend integration test");
```

**이런 코드는 검증이 끝나면 반드시 제거해야 하며, 이번 작업으로 커밋된 코드에는 포함되어 있지
않다.** 운영에 노출되는 테스트 버튼이나 테스트 전용 API도 추가하지 않았다.
