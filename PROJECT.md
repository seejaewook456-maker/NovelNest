# 프로젝트 기획서 - 작가의 AI 비서 (Writer's AI Assistant)

너는 이제부터 이 프로젝트의 시니어 소프트웨어 아키텍트이자 기술 리드다.

내가 만들 프로젝트는 단순한 AI 글쓰기 서비스가 아니다.

핵심 목표는 "AI가 소설을 대신 써주는 서비스"가 아니라 "작가가 긴 작품을 집필할 때 설정, 인물, 사건, 세계관을 기억하고 관리해주는 AI 비서"를 만드는 것이다.

프로젝트의 최종 비전은 다음과 같다. 

* 웹소설 작가
* 소설가
* 시나리오 작가
* 라이트노벨 작가

등이 장편 작품을 집필할 때 발생하는 문제를 해결한다.

---

# 해결하려는 문제

장편 소설을 쓰다 보면 다음 문제가 발생한다.

1. 설정을 잊어버린다.

예시:

* 3화에서 검술을 못 쓴다고 설정
* 15화에서 아무 설명 없이 검술 고수 등장

2. 등장인물 정보를 잊어버린다.

예시:

* 나이
* 성격
* 말투
* 직업
* 관계

3. 사건 순서를 잊어버린다.

예시:

* 이미 죽은 인물이 다시 등장
* 시간 순서 오류

4. 세계관이 충돌한다.

예시:

* 마법 사용 조건 불일치
* 국가 설정 충돌
* 조직 설정 충돌

5. 작품 규모가 커질수록 관리가 어려워진다.

AI가 작가의 "제2의 기억 장치" 역할을 해야 한다.

---

# 핵심 철학

절대로 AI가 소설을 대신 쓰는 서비스가 되어서는 안 된다.

우선순위는 다음과 같다.

1순위 작가의 기억 보조

2순위 설정 관리

3순위 모순 탐지

4순위 문체 분석

5순위 브레인스토밍

소설 자동 생성은 핵심 기능이 아니다.

---

# 기술 스택

## Backend

* Java
* Spring Boot
* Spring Security
* JWT
* JPA
* MySQL

## Frontend

* React

## AI

* OpenAI API

## 배포

* Docker
* AWS EC2
* 운영 도메인: 프론트 `https://novelnestia.com`, 백엔드 `https://api.novelnestia.com`
* 운영 서버 실행 시 `.env`에 `SPRING_PROFILES_ACTIVE=prod` 필수 (미설정 시 기본 프로필 `local`로 기동되어 개발용 DB 설정을 사용하게 됨)

---

# 현재 진행 상황

## 현재 구현 완료

### Backend
* 회원가입 / 로그인 / JWT 인증
* 작품(Novel) CRUD
* 회차(Episode) CRUD
* 등장인물(Character) CRUD
* 세계관(WorldSetting) CRUD
* OpenAI 연동 (global/ai — gpt-4.1-mini, Responses API)
* 회차 요약(EpisodeSummary) — AI 요약 생성 / 조회 (upsert)

### Authentication
* Google OAuth 로그인 (Spring Security OAuth2 Client)
  - Provider enum (LOCAL/GOOGLE) — 향후 KAKAO/NAVER 동일 패턴 확장 가능
  - CustomOAuth2UserService — Google 계정 자동 회원가입, LOCAL 이메일 충돌 시 에러
  - OAuth2AuthenticationSuccessHandler — JWT 발급 후 `/oauth2/callback?token=...` 리다이렉트
  - OAuth2AuthenticationFailureHandler — 에러 메시지 URL 인코딩 후 `/login?error=...` 리다이렉트
  - SessionCreationPolicy.IF_REQUIRED — OAuth2 state 파라미터 세션 저장 필요
* 회원 탈퇴 후 재가입 제한 정책 — 탈퇴 후 설정된 기간(기본 14일, `app.user.rejoin.block-days`) 동안은
  동일 이메일/소셜 계정(provider+providerId)으로 재가입이 차단되고, 기간 경과 후에는 기존 회원을
  복구하지 않고 새 User를 생성해 재가입을 허용한다(이메일 회원가입/Google/Kakao 공통 적용).

### Frontend (MVP 1차)
* React 19 + TypeScript + Vite (frontend/ 폴더)
* 로그인 페이지 (JWT localStorage 저장, Google OAuth 버튼, OAuth 에러 메시지 표시)
* 회원가입 페이지 (Google OAuth 버튼)
* OAuth2 콜백 페이지 (OAuth2CallbackPage — token 파라미터 처리 후 /novels 이동)
* 작품 목록 페이지
* 작품 생성 페이지
* 인증 가드 (PrivateRoute)
* CORS 설정 (CorsConfig — localhost:5173 허용)

### Frontend (MVP 2차)
* 작품 상세 페이지 (NovelDetailPage)
* 회차 목록 페이지 (EpisodeListPage)
* 회차 생성 페이지 (EpisodeCreatePage)
* 회차 상세/수정/삭제 페이지 (EpisodeDetailPage)
* 등장인물 관리 페이지 (CharacterPage — 인라인 CRUD)
* 세계관 관리 페이지 (WorldSettingPage — 인라인 CRUD)
* 라우터 확장 (8개 페이지 전체 연결)

### Frontend (AI 기능 UI)
* 회차 요약 UI — AI 요약 생성/재생성, Emerald 컬러 섹션
* 등장인물 AI 추출 UI — CharacterReviewPage (단계별 검토)
  - 신규/기존 인물 구분 카드
  - newInsights 하이라이트 표시
  - 등록/업데이트/건너뛰기 플로우

### Frontend (디자인 시스템)
* Warm Brown + Cream 컬러 토큰 (CSS 변수)
* 공통 컴포넌트: Button, Card, PageHeader, EmptyState, LoadingSpinner, ProgressBar, BackLink
* AI 기능 전용 Emerald 컬러 시스템
* 전 페이지 리디자인 — 원고 작성 도구 컨셉
* 회차 본문 serif 폰트 적용
* CharacterReview ProgressBar 추가

### Backend (AI 기능)
* 등장인물 AI 추출 (CharacterExtraction) — 후보 생성, DB 저장 없음
  - POST /api/episodes/{episodeId}/character-extraction
  - 신규/기존 인물 구분, newInsights(새 발견 정보) 반환
* 회차 요약(EpisodeSummary) — AI 요약 생성 / 조회 (upsert)
  - POST /api/episodes/{episodeId}/summary
  - GET /api/episodes/{episodeId}/summary

### Backend (회차-등장인물 연결)
* EpisodeCharacter 엔티티 — Episode : Character N:M 연결 테이블
  - POST /api/episodes/{episodeId}/characters/{characterId} — 회차-인물 연결 생성 (중복 무시)
  - GET /api/episodes/{episodeId}/characters — 해당 회차 추출 인물 목록 조회
* AI 추출 후 저장 시 자동 연결 — 회차별 추출 인물 영구 보존

### Frontend (회차별 인물 박스)
* 회차 상세 페이지 "AI 등장인물 추출" 박스 — 해당 회차에서 저장된 인물만 표시
* 인물 카드: 이름, 역할, AI 추출 배지
* 새로고침 후에도 유지 (DB 기반)
* CharacterReviewPage — 저장 후 episodeCharacterApi로 회차-인물 연결 자동 생성

### Backend (삭제 안전성)
* 소설/회차/등장인물 삭제 시 FK 제약 순서에 맞춰 하위 엔티티 cascade 삭제 처리
  - NovelService: EpisodeCharacter → EpisodeSummary → Episode → Character → WorldSetting → Novel 순서로 삭제
  - EpisodeService: EpisodeCharacter → EpisodeSummary → Episode 순서로 삭제
  - CharacterService: EpisodeCharacter → Character 순서로 삭제
* `CODING_CONVENTIONS.md` 작성 — FK 삭제 순서, /error permitAll, GlobalExceptionHandler, fetchWithAuth 동작 규칙 문서화

### Frontend (세계관 AI 추출 UI)
* 회차 상세 페이지 "AI 세계관 추출" 버튼 — 추출 후 WorldSettingReviewPage로 이동
* WorldSettingReviewPage — 설정 후보 1개씩 검토/수정/저장 플로우
  - 신규 설정: category select + title + content 수정 후 저장 → POST /api/novels/{novelId}/world-settings
  - 기존 설정 보강: 기존 내용 + newInsights 강조 표시 → PATCH /api/world-settings/{id}
  - 완료 화면: 신규 저장 / 기존 보강 / 건너뜀 통계 표시
* Character Extraction UI와 동일한 UX 원칙 적용 (1개씩 검토, AI 결과 자동 저장 없음)

### Backend (세계관 AI 추출)
* WorldSetting Extraction — POST /api/episodes/{episodeId}/world-setting-extraction
  - 회차 본문 + 작품 정보 + 기존 WorldSetting 목록을 AI에 전달
  - 신규 설정(isExistingSetting=false) / 기존 설정 보강(isExistingSetting=true) 구분
  - newInsights: 기존 content 대비 새롭게 발견된 정보 목록
  - DB 저장 없음 — 사용자 검토 후 기존 WorldSetting CRUD API로 저장

### Backend (설정 충돌 탐지)
* Conflict Detection — POST /api/episodes/{episodeId}/conflict-detection
  - 현재 회차 본문 + 등장인물 목록 + 세계관 설정 목록 + 최근 10개 회차 요약을 AI에 전달
  - 충돌 유형(CHARACTER/PERSONALITY/RELATIONSHIP/WORLD_SETTING/ABILITY/TIMELINE) 분류
  - 심각도(HIGH/MEDIUM/LOW) 포함
  - DB 저장 없음, 읽기 전용 분석 — 자동 수정 없음

### Frontend (설정 충돌 감지 UI)
* 회차 상세 페이지 "설정 충돌 감지" 섹션 — 인라인 결과 표시
  - [설정 충돌 감지] 버튼 → POST /api/episodes/{episodeId}/conflict-detection
  - 요약 바: 총 N건, HIGH/MEDIUM/LOW 건수 배지
  - 충돌 카드: severity 배지, type 한국어 라벨, 기존 설정 / 현재 회차 / AI 설명 / AI 제안
  - 충돌 없음: 빈 상태 메시지 표시
  - 재분석: 버튼 재클릭으로 새 결과로 덮어씀
  - severity 색상: HIGH(Red) / MEDIUM(Amber) / LOW(Warm Brown)

### UX 개선

* Episode Content Copy — 회차 상세 페이지 본문 영역 오른쪽 상단 [📋 본문 복사] 버튼
* Novel/Episode 삭제 확인 절차 — 삭제 버튼 클릭 시 ConfirmDeleteModal 표시, "삭제하겠습니다" 문구 입력 후 삭제 활성화 (실수 삭제 방지)
* Episode Detail Page AI tools quick scroll — 본문 헤더 [▼ AI 도구로 이동] 버튼, useRef + scrollIntoView smooth scroll
  - navigator.clipboard.writeText 사용, 예외 처리 포함
  - 복사 성공/실패 시 하단 고정 Toast(Snackbar) 표시 (2초 자동 소멸)
  - 버튼 텍스트 2초간 "✓ 복사됨"으로 변경 후 원복
  - 모바일 대응 (@media max-width: 480px Toast 좌우 여백 처리)
  - 백엔드 수정 없음, 프론트엔드 전용
* Collapsible Create Form — 등장인물/세계관 관리 페이지 생성폼 접이식 처리
  - 페이지 진입 시 "＋ 새 인물/설정 추가" 버튼만 표시 (폼 숨김)
  - 클릭 시 폼 펼쳐짐, 저장 성공/취소 시 폼 자동 닫힘
  - CollapsibleFormCard 공통 컴포넌트 (children 방식)
* Favorite Character / WorldSetting — 즐겨찾기(★) 영구 저장, DB 컬럼 isFavorite 추가
  - Character: 즐겨찾기 우선 정렬(isFavorite DESC → name ASC), 카드 헤더 별 아이콘 버튼
  - WorldSetting: 카테고리 상세 뷰 카드 헤더 별 아이콘 버튼, 카테고리 내 즐겨찾기 우선 정렬
  - PATCH /api/characters/{id}/favorite, PATCH /api/world-settings/{id}/favorite
  - 토글 즉시 재정렬 + Toast 피드백, 백엔드 ddl-auto:update로 컬럼 자동 추가
* Episode Detail Page Scroll to Top — AI 도구 영역 하단 [▲ 최상단으로 이동] 버튼
  - AI 도구 영역 최하단에 위치, 클릭 시 window.scrollTo smooth scroll
  - 상단 [▼ AI 도구로 이동] 버튼과 대응되는 구조
  - ghost 버튼 스타일, 디자인 일관성 유지, 백엔드 수정 없음
* WorldSetting Category Grouped View — 세계관 설정 목록을 카테고리별 카드로 그룹화
  - 기본 화면: 데이터가 있는 카테고리 카드만 표시 (카테고리명 + 설정 수)
  - 카드 클릭 시 해당 카테고리의 설정 목록 상세 표시
  - 상세 화면에서 수정/삭제 가능, 뒤로가기 버튼으로 카테고리 목록 복귀
  - settings useMemo grouping으로 생성/수정/삭제 즉시 카운트 반영
  - 카테고리 상세에서 모든 설정 삭제 시 자동으로 카테고리 목록으로 복귀
  - 백엔드 수정 없음, 프론트엔드 전용
* Episode 글쓰기 보조 툴바 (WritingAssistToolbar) — 새 회차 작성 / 회차 수정 공통 적용
  - Plain text 유지 원칙 (Markdown/HTML 저장 없음, AI 기능에 영향 없음)
  - 글자 수 실시간 표시 — 공백 제외 / 공백 포함 글자 수
  - 구분선 삽입 — 커서 위치에 ────────────── 삽입, 앞뒤 줄바꿈 자동 처리
  - 특수문자 빠른 입력 — ……/…/『』/「」/〈〉/― 버튼
  - 괄호형 특수문자(『』「」〈〉) 클릭 시 커서 자동으로 괄호 가운데 이동
  - 선택 영역 있으면 삽입 문자로 대체, 없으면 커서 위치 삽입
  - 공통 컴포넌트(WritingAssistToolbar.tsx) — props: content / onChange / textareaRef
  - 구분선/기호 삽입 시 caret·스크롤 위치 유지 (버그 수정)
    - 원인: controlled textarea의 value를 코드로 갱신하면(React가 DOM value를 직접 set) 브라우저가 scrollTop을 0으로, 커서를 텍스트 끝으로 되돌리는 부작용 발생
    - 툴바 버튼에 `onMouseDown` preventDefault를 걸어 클릭해도 textarea가 blur되지 않도록 함(포커스/selection 유지)
    - 삽입 직전 scrollTop/scrollLeft/selectionStart/End를 저장해두고, value 갱신 후 `requestAnimationFrame`에서 `focus({ preventScroll: true })` + `setSelectionRange` + scrollTop/scrollLeft 복원
    - 회차 작성/수정 화면 모두 동일 컴포넌트를 사용하므로 한 번의 수정으로 양쪽에 적용됨
* Episode Detail Page 반응형 2열 레이아웃 — 회차 본문 / AI 도구를 독립된 박스로 분리
  - 브레이크포인트 **1200px** 이상: `.episode-detail-layout`이 CSS Grid(`minmax(0,3fr) minmax(0,2fr)`)로 좌우 2열 배치 — **본문 60% : AI 도구 40%** 비율
    - `.main-content`(최대 800px)로는 두 박스가 나란히 표시될 폭이 부족해, `.episode-workspace`(회차 작성/수정 화면)와 동일한 breakout 기법(width:100vw + max-width + left:50% + translateX(-50%))으로 뷰포트 기준 폭을 확장
    - 1200px은 두 박스가 각각 실질적인 폭을 확보할 수 있는 지점으로 선택 (본문 가독성 + AI 도구 폼/카드 모두 고려)
  - 1200px 미만: 세로 스택 유지(본문 박스 → AI 도구 박스), 두 박스는 동일하게 독립된 카드로 표시
  - **AI 도구 순서** — 화면 크기와 무관하게 항상 동일한 DOM 순서: ① AI 회차 요약 → ② 설정 충돌 감지 → ③ AI 등장인물 추출 → ④ AI 세계관 추출 (설정 충돌 감지를 인물/세계관 추출보다 먼저 실행하도록 권장하는 흐름에 맞춰 배치, `order` 없이 렌더링 순서 자체를 정렬)
  - **인물/세계관 추출 결과 카드**(`.episode-character-list`) — 화면 크기와 무관하게 항상 한 행에 1개(`grid-template-columns: minmax(0,1fr)`). 카드 내 "AI 추출" 배지는 제거됨(이름/역할만 표시)
  - **`← 회차 목록` 버튼** — 1200px 이상에서는 `.episode-detail-back-link`가 `position:absolute`로 AI 도구 박스 우측 상단(그리드 컨테이너 기준 top/right 36px)에 재배치되어 AI 도구 박스 헤더와 겹쳐 보이도록 배치(그리드 자동 배치에서 제외되어 본문/AI 도구 컬럼 배치에 영향 없음). 1200px 미만에서는 기존처럼 레이아웃 맨 위에 정적으로 표시. 컴포넌트는 한 곳에서만 렌더링하고 CSS로만 재배치(중복 렌더링 없음)
  - `▼ AI 도구로 이동` / `▲ 최상단으로 이동` 버튼: 1200px 이상(2열 동시 노출)에서는 각각 `.ai-tools-jump-btn`, `.scroll-to-top-btn`에 `display: none`으로 완전히 숨김(공간 차지 없음), 1200px 미만에서는 기존과 동일하게 표시 + 스크롤 이동 동작 유지
  - 순수 CSS 미디어쿼리로 처리 — `window.innerWidth` 감시 없음
  - 회차 조회/수정/저장/삭제, AI 요약/설정 충돌 감지/등장인물·세계관 추출의 API 호출·로딩·오류 처리 로직은 변경 없음 (레이아웃과 표시 순서만 변경)
  - 관련 파일: `frontend/src/pages/EpisodeDetailPage.tsx`, `frontend/src/App.css`

### Frontend (GA4 애널리틱스)
* react-ga4 기반 GA4 연동 — `src/lib/analytics.ts`(initializeAnalytics/trackPageView/trackEvent), `src/constants/analyticsEvents.ts`(이벤트명 상수)
* React Router(`createBrowserRouter`) 페이지 이동 자동 추적 — `router.subscribe`로 구현, 동적 라우트는 `/novels/:novelId`처럼 라우트 패턴으로 정규화, 쿼리스트링은 미전송(토큰 등 민감정보 유출 방지)
* sign_up / login(email·google·kakao) / novel_create / episode_create·update·copy / ai_summary_run / ai_conflict_check_run / ai_character_extract_run / ai_worldview_extract_run / ai_chat_message_send / account_delete 이벤트를 각 API 성공 시점에 전송(실패 시 미전송)
* 이벤트 파라미터에 개인정보·사용자 콘텐츠(제목/본문/AI 응답 등) 미포함
* 상세 내용은 `FRONTEND_API.md`의 "GA4 애널리틱스 연동" 섹션 참고

### Frontend (Microsoft Clarity)
* `src/lib/clarity.ts`의 `initializeClarity()`를 `main.tsx` 진입점에서 1회 호출 — Clarity 공식 추적 스크립트를 동적 삽입, 스크립트 태그 존재 여부 + 플래그로 중복 삽입 방지, 실패해도 앱 렌더링에 영향 없음
* `VITE_CLARITY_PROJECT_ID`가 없으면 조용히 초기화를 건너뜀 — `.env.development`/`.env.production`에 값을 커밋하지 않고 Vercel Production 환경변수로만 관리해 로컬 개발에서는 항상 비활성화
* 로그인/회원가입/작품/회차/등장인물/세계관/AI 채팅/설정 충돌 결과 등 사용자 작성·AI 생성 콘텐츠가 표시되는 컨테이너에 `data-clarity-mask="true"` 적용, 버튼명/메뉴명 등 일반 UI는 마스킹 제외
* GA4는 페이지 이동마다 이벤트를 보내야 해 `router/index.tsx`에서 초기화하지만, Clarity는 스크립트 1회 삽입이 전부라 `main.tsx`에서 독립적으로 초기화 — 두 모듈 간 충돌 없음
* 상세 내용은 `FRONTEND_API.md`의 "Microsoft Clarity 연동" 섹션 참고

### Sentry (프론트엔드/백엔드 운영 오류 수집)
* 프론트엔드: `@sentry/react` — `src/lib/sentry.ts`(initializeSentry/captureApiError), `src/components/AppErrorBoundary.tsx`(렌더링 오류 Fallback UI), `main.tsx`에서 GA4/Clarity와 함께 1회 초기화
* 프론트엔드는 `fetchWithAuth.ts` 한 곳에서만 API 오류를 캡처(500 이상/네트워크 오류만, 4xx 제외) — 개별 API 함수 수정 없음
* 백엔드: `io.sentry:sentry-spring-boot-starter-jakarta:8.50.1`, `application-prod.yml`에만 `sentry:` 설정 추가(local/test 미적용)
* 백엔드는 `GlobalExceptionHandler`의 catch-all(Exception.class)이 Sentry 자동 수집을 사실상 막는 구조라, 그 지점 + AsyncConfig(비동기 예외) + VerificationCleanupScheduler(정리 작업) + OpenAiService(외부 API 실패) 4곳에서만 수동 캡처 — BusinessException 등 정상 비즈니스 예외는 미전송
* Source Map, 성능 모니터링(tracesSampleRate), 분산 추적, Sentry Replay, Release 연동은 이번 범위 아님(Replay는 Clarity와 중복이라 의도적으로 미적용)
* Docker Compose는 `env_file`로 `.env` 전체를 이미 전달하므로 별도 수정 없음 — EC2 `.env`에 `SENTRY_DSN`/`SENTRY_ENVIRONMENT` 추가 후 컨테이너 재생성(`docker compose up -d`) 필요, 단순 restart로는 반영 안 됨
* 상세 내용은 `docs/SENTRY.md` 참고

### AI 하루 사용량 제한 (사용자별 일일 제한 + 잔여 횟수 표시)
* 백엔드: `domain/aiusage` 패키지 신규 — `AiFeatureType` enum(5개 기능 중앙 관리), `AiDailyUsage` 엔티티(사용자별·날짜별 카운터, `user_id`+`usage_date` 유니크), `AiUsageService`(검사·원자적 증가·조회 단일 창구), `AiUsageLimitProperties`(제한값 환경변수 바인딩)
* 기본 제한: 요약 20회, 충돌감지·인물추출·세계관추출 각 15회, 챗봇 20회(최초 50회 → 하향 조정) — `AI_DAILY_*_LIMIT` 환경변수로 조정. **전체 합계 제한은 두지 않음**(최초에 100회 합계 제한도 있었으나 불필요하다고 판단해 제거, 기능별 제한만 독립적으로 적용)
* Asia/Seoul 자정 초기화는 스케줄러 없이 `(user_id, usage_date)` 유니크 제약으로 구현 — 날짜가 바뀌면 새 행이 자연스럽게 생성됨, `Clock` 빈 주입으로 테스트 가능
* 동시성은 조건부 UPDATE(`WHERE count < limit`)로 원자적 처리, `checkAndIncrement`는 `REQUIRES_NEW`로 OpenAI 호출 직전 커밋 — 호출 실패해도 이미 차감된 횟수는 복구하지 않음. 개발 중 발견한 Hibernate 세션 오염 문제로 행 생성 로직을 별도 빈(`AiDailyUsageRowInitializer`)으로 분리
* `GET /api/ai/usage/daily` 조회 API 추가, 제한 초과 시 429(`AI_DAILY_FEATURE_LIMIT_EXCEEDED`)
* 프론트엔드: `useAiDailyUsage` 공통 훅 + `aiUsageStore`(pub-sub 캐시) + `AiUsageHint` 공통 컴포넌트 — 회차 상세 AI 도구 4종 + 3개 페이지가 공유하는 `AiChatPanel`에 잔여 횟수 표시 및 0일 때 버튼 비활성화
* Flyway `V6__add_ai_daily_usage.sql`(테이블 생성) → `V7__remove_ai_daily_usage_total_count.sql`(전체 합계 컬럼 제거) — 로컬 실제 MySQL에 순서대로 적용 및 스키마 확인 완료
* 상세 내용은 `docs/AI_USAGE_LIMIT.md` 참고

### AI 분당 Rate Limit (사용자당 최근 60초 10회, 5개 기능 전체 합계 기준)
* 목적: 하루 사용량 제한만으로는 막지 못하는 "짧은 시간에 몰아서 요청"하는 남용을 방지. 하루 사용량 검사보다 먼저 실행되며, 여기서 막히면 하루 사용 횟수는 차감되지 않고 OpenAI도 호출되지 않음
* 백엔드: `domain/airatelimit` 패키지 신규 — `AiRequestLog` 엔티티(요청 시각 로그, `id`/`user_id`/`requested_at` 최소 컬럼), `AiRateLimitService`(검사·기록 단일 창구, `checkAndRecord(userId)`), `AiRateLimitProperties`(`app.ai.rate-limit.max-requests`/`window-seconds`, 기본 10/60)
* Redis 없이 DB(H2/MySQL)만으로 구현 — 최근 60초 요청 수를 COUNT로 계산하는 Sliding Window 방식, 기능별이 아니라 "AI 전체 요청" 합계로 판단
* 동시성은 `users` 테이블 해당 행에 비관적 락(`SELECT ... FOR UPDATE`)을 걸어 "조회~기록" 구간을 직렬화, `checkAndRecord`는 `REQUIRES_NEW`로 즉시 커밋되어 잠금을 짧게 유지
* 오래된 로그는 별도 스케줄러 없이 매 요청마다 윈도우 밖으로 나간 행을 함께 삭제(`deleteByRequestedAtBefore`)해 테이블 크기를 제한
* 제한 초과 시 429(`AI_RATE_LIMIT_EXCEEDED`) — 프론트엔드는 기존 공통 오류 표시 경로(`err.message`)를 그대로 타므로 별도 코드 변경 없이 처리됨
* Flyway `V8__add_ai_request_logs.sql`(테이블 생성, `users` FK + 인덱스 2개)
* 상세 내용은 `docs/AI_RATE_LIMIT.md` 참고

### 일회성 데이터 정리 로직 점검 (이메일 인증번호 / Refresh Token / AI 요청 로그)
* 세 데이터 모두 정리 로직이 이미 구현되어 있음을 확인 — 코드 변경 없음(점검 및 문서화만 수행)
* 이메일 인증번호: 재발급 시 기존 행 갱신(`renew`, 중복 불가 — `(email, purpose)` 유니크), 회원가입 완료 시 즉시 소비(`consume`), 만료 행은 `VerificationCleanupScheduler`가 매 정각 정리
* Refresh Token: 별도 테이블이 아닌 `users.refresh_token` 단일 컬럼이라 애초에 누적이 불가능, 로그아웃/탈퇴 시 즉시 `null`, 재발급 시 Rotation으로 즉시 교체
* AI 요청 로그: 위 AI 분당 Rate Limit 절 참고 — 매 요청 시 윈도우 밖 로그를 함께 삭제하는 방식으로 이미 구현됨
* 상세 내용은 `docs/DATA_CLEANUP.md` 참고

### 메모(Memo) 관리 기능 추가
* 작품별 개인 텍스트 메모장 — 아이디어/복선/장면 구상/TODO를 자유롭게 기록. 회차와 달리 순번(episodeNumber) 개념이 없고, **AI 도구(요약/충돌감지/인물·세계관 추출/챗봇)를 전혀 사용하지 않는 순수 텍스트 공간**이다
* 백엔드: `domain/memo` 패키지 신규 — `Memo` 엔티티(`novel_id` FK + title + content(TEXT), `BaseEntity` 상속), `MemoRepository`/`MemoService`/`MemoController`/DTO 3종. Episode/Character와 동일하게 `novel.getUser()`로 소유권 검증(`IllegalArgumentException`=404, `SecurityException`=403)
* API: `POST/GET /api/novels/{novelId}/memos`, `GET/PATCH/DELETE /api/memos/{memoId}` — Episode API와 동일한 경로 규칙
* Novel 삭제 시 하위 리소스 정리 순서(`NovelService.deleteNovel`)에 `memoRepository.deleteAllByNovel(novel)` 추가 — Character/WorldSetting과 동일한 정책(명시적 선삭제, FK CASCADE 미사용)
* Flyway `V9__create_memos_table.sql` — V6~V8과 동일하게 `information_schema` 존재 확인 + `PREPARE`/`EXECUTE` 동적 DDL 패턴, `(novel_id, updated_at)` 인덱스(목록을 최근 수정순으로 조회하기 위함)
* 프론트엔드: `types/memo.ts` + `api/memoApi.ts`(episodeApi.ts와 동일한 형태) + `MemoListPage`/`MemoCreatePage`/`MemoDetailPage`(상세 화면에서 인라인으로 수정 토글, Episode처럼 별도 `/edit` 라우트를 두지 않음) — 라우트: `/novels/:novelId/memos`, `/novels/:novelId/memos/new`, `/memos/:memoId`
* Episode 작성/수정 화면의 글자 수 계산, 구분선/기호 삽입 도구, AI 도구 전체를 의도적으로 제외 — 단순 제목 input + 내용 textarea만 제공
* 작품 상세 페이지(`NovelDetailPage`)에 "메모 관리" 카드 추가, 관리 카드 레이아웃을 `.section-cards` 1×3 → **2×2 grid**로 변경(순서: 회차 관리 / 등장인물 관리 / 세계관 관리 / 메모 관리), 480px 이하에서는 1열로 재배치
* 신규 CSS는 `.memo-list`/`.memo-item`/`.memo-detail` 등 — 기존 `--color-bg-card`/`--color-border`/`--color-text-*` 등 다크 모드 대응 토큰만 재사용해 별도 다크 모드 오버라이드 없이 라이트/다크 모두 자동 대응
* 백엔드 테스트: `MemoServiceTest`(Mockito) — 생성/여러 개 생성/목록/상세/수정/삭제/타 사용자 접근 차단/존재하지 않는 ID 시나리오 검증

### 메모 즐겨찾기 + 회차 작성/수정 메뉴 통합 + 관리 버튼 재배치
* 메모 즐겨찾기: `Memo.isFavorite`(Boolean, 기본값 false) 추가 — Character/WorldSetting의 `isFavorite` 필드·`PATCH /favorite` API·`MemoFavoriteRequestDto` 패턴을 그대로 재사용. Flyway `V10__add_favorite_to_memos.sql`(`information_schema` 컬럼 존재 확인 후 `ALTER TABLE ... ADD COLUMN ... DEFAULT 0` 동적 실행)
* 정렬 통일: `MemoRepository.findAllByNovelOrderByIsFavoriteDescUpdatedAtDesc` 하나로 목록 정렬을 백엔드에 고정 — 메모 관리 페이지와 회차 작성/수정 메뉴가 같은 API를 호출하므로 항상 같은 순서를 본다. 프론트 낙관적 재정렬(즐겨찾기 토글 직후 refetch 없이 즉시 위치 이동)에도 `frontend/src/utils/memoSort.ts`의 `sortMemos` 하나만 두 화면(`MemoListPage`, `MemoReferencePanel`)이 공유해, 각자 다른 정렬 로직을 중복 작성해 순서가 어긋날 여지를 없앴다
* 회차 작성/수정 메뉴: `EpisodeToolRail`의 `EpisodeWorkspacePanelKey`에 `'memos'` 추가, 메뉴 순서를 **메모 → 등장인물 → 세계관 → AI 채팅**으로 배치. 아이콘은 새 라이브러리 없이 기존 인라인 SVG 컨벤션 그대로 FileText 형태 아이콘 추가. `MemoReferencePanel`(신규) — `CharacterReferencePanel`/`WorldSettingReferencePanel`과 동일한 구조(검색, 인라인 확장으로 상세 열람, `전체 관리 ↗` 새 탭 링크, 즐겨찾기 토글)로 조회 전용 CRUD만 재사용. 메모 작성/수정/삭제는 이 패널에서 제공하지 않음(관리 페이지 전용)
* 작품 상세 페이지 2×2 버튼: DOM 순서를 회차 관리 → 메모 관리 → 등장인물 관리 → 세계관 관리로 바꿔 1행 [회차][메모], 2행 [등장인물][세계관] 배치. `.section-card` 세로 padding을 26px→14px로 줄여 카드가 더 슬림해짐(가로 크기·grid 구조·border/hover/shadow는 그대로 유지, 클릭 영역은 여전히 충분)
* 다크 모드: 신규 UI 전부 기존 `--color-*` 토큰과 `.favorite-btn`/`.item-card`/`.workspace-ref-*` 공통 클래스만 재사용 — 별도 오버라이드 추가 없음

### 회차 작성/수정 메뉴 패널 — 메모/이전 회차/AI 채팅 탭 넓은 레이아웃
* 메모/이전 회차/AI 채팅은 카드형 정보(등장인물/세계관)보다 긴 텍스트를 읽는 비중이 높아, 화면 폭이 충분한 대형 데스크톱에서는 우측 패널을 더 넓게 사용하도록 개선. 등장인물/세계관 탭은 기존 380px 패널·카드 배치를 그대로 유지
* 구현: `EpisodeWorkspace`가 `activePanel`이 `'memos'`/`'previousEpisodes'`/`'chat'`일 때 최상위 컨테이너에 `panel-wide` 클래스를 추가. CSS는 `.episode-workspace.panel-wide`의 `--ep-panel-width`를 `clamp(380px, 30vw, 640px)`로 덮어쓰기만 함 — 이 변수 하나를 이미 `.episode-workspace-panel-wrap.open`/`.episode-workspace-panel`의 width와 `.episode-workspace.panel-open`의 breakout `max-width` calc가 모두 참조하고 있어, 새 위치 계산 코드 없이 회차 본문 입력 박스·메뉴 레일·패널이 전체 레이아웃(breakout 중앙 정렬) 차원에서 함께 재배치된다(패널이 넓어진 만큼 중앙 정렬 축이 이동해 입력 박스가 자연스럽게 왼쪽으로 밀림, 본문과 겹치지 않음)
* 고정 breakpoint 하나로 전환하는 대신 `vw` 기반 `clamp`로 화면 폭에 비례해 연속적으로 넓어지게 구현 — 처음에 특정 px 이상에서만 켜지는 방식으로 시도했더니 그 기준보다 좁은(그러나 결코 좁지는 않은) 일반적인 노트북 브라우저 창 폭에서는 패널이 전혀 안 넓어져 보이는 문제가 있어 변경. 하한 380px(=등장인물/세계관과 동일한 기존 폭)로 좁은 화면에서 더 좁아지지 않게, 상한 640px로 초대형 모니터에서 과도하게 넓어지지 않게 막음. 30vw 계수는 "패널을 제외한 나머지 고정 폭 합(입력 박스 680 + gap 20×2 + 메뉴 96 + breakout 여백 48 = 864px)에 0.30·VW를 더해도 VW(뷰포트 폭)보다 항상 작다"는 조건이 VW≈1235px부터 성립하도록 역산한 값 — 기존 1300px 오버레이 전환 breakpoint보다 낮은 지점이라 가로 스크롤 없이 항상 안전
* 패널 내부(`MemoReferencePanel`/`AiChatPanel`의 목록·카드·채팅 말풍선·입력창)는 이미 고정 px 폭이 아닌 상대 폭(`width:100%`, `flex:1`, `max-width:80%` 등)으로 구현돼 있어 패널 컴포넌트 자체는 수정하지 않고도 넓어진 폭에 자동으로 맞춰짐

### 회차 작성/수정 메뉴 — "이전 회차" 참고 패널 추가
* 회차 작성/수정 중 페이지 이동 없이 이전 회차 본문을 참고할 수 있는 조회 전용(Read-only) 패널 추가. 메뉴 순서: **메모 → 이전 회차 → 등장인물 → 세계관 → AI 채팅**(`EpisodeToolRail`의 `EpisodeWorkspacePanelKey`에 `'previousEpisodes'` 추가, 아이콘은 새 라이브러리 없이 기존 인라인 SVG 컨벤션 그대로 열린 책 모양 추가)
* 범위 필터링: 현재 작성/수정 중인 회차의 `episodeNumber`보다 작은(`<`) 회차만 오름차순으로 노출. ID 크기가 아니라 실제 회차 순서 필드(`episodeNumber`)를 기준으로 필터링·정렬(기존 `findAllByNovelOrderByEpisodeNumberAsc`와 동일한 정렬 기준 재사용). 새 회차 작성 화면은 사용자가 입력 중인 회차 번호 입력창의 실시간 값을, 수정 화면은 autoSave payload(`Number(editEpisodeNumber) || episode.episodeNumber`)와 동일한 폴백 규칙을 그대로 재사용해 기준 번호로 삼는다 — 아직 번호를 입력하지 않은 새 회차 작성 화면에서는 무엇이 "이전"인지 알 수 없으므로 전체 회차를 보여준다
* 성능 고려 — 신규 경량 API 추가: 기존 `GET /api/novels/{novelId}/episodes`는 회차 목록 조회 시 각 회차의 전체 본문(content)까지 함께 반환해, 회차가 수백 개인 작품에서는 "이전 회차" 목록만 보여주는 데도 불필요하게 무거운 응답이 된다. 이를 피하기 위해 본문 없이 번호+제목만 반환하는 `GET /api/novels/{novelId}/episodes/brief`(`EpisodeBriefResponseDto`)를 새로 추가 — 목록에서는 이 경량 API만 쓰고, 사용자가 특정 회차를 클릭했을 때만 기존 `GET /api/episodes/{episodeId}` 상세 API를 그대로 재사용해 본문을 가져온다(상세 조회용 신규 API는 만들지 않음). 두 API 모두 기존 `validateOwner`(작품 소유자만 접근 가능) 검증을 그대로 재사용
* UI 구조: `PreviousEpisodesPanel`(신규) — 회차 본문이 길어 `MemoReferencePanel`의 인라인 아코디언 방식 대신, 목록 화면과 상세 화면을 완전히 분리하고 `BackLink`(기존 "← 회차 목록" 컴포넌트 재사용)로 목록에 돌아가는 방식을 사용. 카드/헤더 스타일은 `.item-card`/`.workspace-ref-card`/`.item-card-header` 등 기존 참고 패널 공통 클래스를 그대로 재사용. 본문 줄바꿈/빈 줄은 메모 상세와 동일하게 `white-space: pre-wrap`(`.episode-ref-content`, `.memo-ref-content`와 동일한 방식)으로 처리. 회차 상세 데이터를 새로 조회할 때는 이전 선택 내용을 즉시 비우고 로딩 상태로 전환해, 다른 회차의 내용이 잘못 표시되지 않게 함
* 1화 작성/수정 시(이전 회차 없음)에는 기존 `EmptyState` 컴포넌트로 안내 문구 표시
* 패널은 메모/AI 채팅과 동일하게 `panel-wide` 넓은 레이아웃 대상에 포함(위 섹션 참고)
* DB 스키마 변경 없음(Flyway 마이그레이션 추가 없음) — 기존 `episodes` 테이블/엔티티를 그대로 조회만 함
* 백엔드 테스트: `EpisodeServiceTest`(신규, Mockito) — 이번에 추가한 `getEpisodeBriefs`만 검증(본문 미포함·번호순 반환, 타 사용자 접근 차단). 기존 회차 CRUD 메서드들은 이전부터 테스트가 없던 영역이라 이번 범위에 포함하지 않음

## 아직 구현되지 않음

### AI 기능 (미구현)
* (없음 — AI MVP 기능 모두 구현 완료)

### 구현하지 않기로 결정
* 문체 분석 — 작가의 문체는 창작 자유 영역이므로 서비스가 평가/교정하는 방향 지양

---

# MVP 범위

## 1. 작품 관리

**Novel**

속성

* id
* user
* title
* genre
* description
* createdAt
* updatedAt

기능

* 작품 생성
* 작품 조회
* 작품 수정
* 작품 삭제

---

## 2. 회차 관리

**Episode**

속성

* id
* novel
* title
* episodeNumber
* content
* createdAt
* updatedAt

기능

* 회차 생성
* 회차 조회
* 회차 수정
* 회차 삭제

---

## 3. 등장인물 관리

**Character**

속성

* id
* novel
* name
* role
* age
* personality
* speechStyle
* description

기능

* 생성
* 조회
* 수정
* 삭제

---

## 4. 세계관 관리

**WorldSetting**

속성

* id
* novel
* category
* title
* content

category 예시

* 국가
* 종족
* 마법
* 조직
* 장소
* 사건
* 아이템

기능

* 생성
* 조회
* 수정
* 삭제

---

## 5. 메모 관리

**Memo**

작품별로 아이디어, 복선, 장면 구상, TODO 등을 자유롭게 기록해두는 개인 텍스트 메모장이다.
회차와 달리 순번(episodeNumber 같은) 개념이 없고, AI 분석·요약·추출 등 AI 도구를 전혀 사용하지
않는다 — 순수하게 사용자가 직접 쓰고 읽는 공간이다.

속성

* id
* novel
* title
* content
* isFavorite (기본값 false — Character/WorldSetting과 동일한 명명·기본값 규칙)
* createdAt
* updatedAt

기능

* 메모 생성
* 메모 목록 조회 (즐겨찾기 우선 → 최근 수정순)
* 메모 상세 조회
* 메모 수정
* 메모 즐겨찾기 설정/해제
* 메모 삭제

회차 작성/수정 화면의 메뉴 레일(메모 → 등장인물 → 세계관 → AI 채팅 순)에서도 메모 목록을
참고용으로 열람할 수 있다 — 목록/상세 확인만 가능하고 수정·삭제는 메모 관리 페이지에서만 한다.

---

# AI 기능 구현 순서

## 1단계

문체 분석

예시

* 반복 표현 탐지
* 문장 길이 분석
* 시점 혼동 탐지
* 대사 비율 분석

---

## 2단계

등장인물 추출

회차 원고를 분석하여

* 이름
* 성격
* 역할

등을 추출

---

## 3단계

회차 요약 생성

**EpisodeSummary**

속성

* summary
* importantEvents
* characterChanges
* settingUpdates

---

## 4단계

설정 충돌 탐지

예시

"3화에서는 마법 사용 불가능 설정인데 12화에서 아무 설명 없이 마법 사용"

"이미 사망한 인물이 재등장"

"등장인물 성격이 기존 설정과 충돌"

---

# 장기 목표

장기적으로는 RAG 구조를 도입한다.

수집 대상

* 작품
* 회차
* 등장인물
* 세계관
* 사건 기록

AI가 분석할 때

* 현재 회차
* 이전 회차 요약
* 등장인물 정보
* 세계관 정보

를 함께 참조하도록 설계한다.

---

# 개발 원칙

1. 과도한 기능 추가 금지

2. MVP 우선

3. 작은 단위로 구현

4. Spring Boot 실무 구조 유지

5. Controller-Service-Repository 계층 분리

6. DTO 사용

7. JPA 사용

8. JWT 기반 인증 유지

9. 확장 가능하도록 설계

10. 항상 다음 구현 우선순위를 제안
