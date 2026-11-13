# 다크 모드 가이드

## 목적

장시간 집필 작업 시 눈의 피로를 줄이기 위해 라이트/다크 2가지 테마를 지원한다.
디자인 방향은 Notion/Linear/GitHub Dark류의 검정+회색 기반 무채색이며, 상태를 나타낼 때만
Blue(Primary)/Green(Success)/Orange(Warning)/Red(Error) 색상을 사용한다.

**라이트 모드의 기존 웜 브라운 브랜드 컬러는 그대로 유지한다.** 위 포인트 컬러 규정은
다크 모드에만 적용된다.

테마는 **시스템(OS) 설정과 동기화하지 않는다.** 처음 방문한 사용자는 항상 라이트로
시작하고, 이후 헤더의 토글 버튼으로 직접 선택한 값만 localStorage에 저장되어 유지된다.
전용 테마 설정 화면은 두지 않는다(헤더 토글이 유일한 진입점).

## 아키텍처

이 프로젝트는 전역 상태에 React Context API를 쓰지 않고, 모듈 단위 pub-sub 스토어(예:
`frontend/src/state/aiUsageStore.ts`)를 `useSyncExternalStore`로 구독하는 방식을 쓴다.
테마 상태도 동일한 컨벤션을 따른다.

| 파일 | 역할 |
|---|---|
| `frontend/src/state/themeStore.ts` | 테마 상태 저장(`'light'` \| `'dark'`, 기본값 `'light'`) + localStorage 영속화 + `<html data-theme>` 반영 |
| `frontend/src/hooks/useTheme.ts` | `useSyncExternalStore`로 `themeStore`를 구독하는 훅. `{ theme, setTheme }` 반환 |
| `frontend/src/components/ThemeToggle.tsx` | 헤더(로그인 후 `MainLayout`, 랜딩 `LandingHeader`)의 ☀️/🌙 토글 버튼(클릭 시 라이트↔다크 전환) |
| `frontend/index.html` | React 로드 전 실행되는 FOUC 방지 인라인 스크립트 |
| `frontend/src/index.css` | CSS 커스텀 프로퍼티(Theme Token) 정의 + 다크 모드 오버라이드 |

## CSS Theme Token

모든 색상은 `frontend/src/index.css`의 `:root`에 정의된 CSS 변수를 통해서만 사용한다.
컴포넌트나 페이지에 색상을 직접 hex/rgba로 하드코딩하지 않는다.

| 토큰 | 용도 |
|---|---|
| `--color-bg` | 페이지 배경 |
| `--color-bg-card` | 카드/헤더/모달 등 표면(surface) |
| `--color-bg-input` | 인풋/텍스트에어리어 배경(카드보다 한 단계 밝게) |
| `--color-bg-hover` | 중립 hover 배경 |
| `--color-text-primary` / `-secondary` / `-muted` | 제목/본문/설명 텍스트 |
| `--color-border` / `--color-border-strong` | 일반 테두리 / 강조 테두리(점선 등) |
| `--color-focus-ring` | 포커스 시 box-shadow 링 |
| `--color-primary` / `-hover` / `-light` | 브랜드/인터랙션 색상(라이트: 브라운, 다크: 블루) |
| `--color-ai` / `-hover` / `-light` / `-border` | AI 관련 강조(다크에서는 primary와 동일 블루 계열) |
| `--color-danger` / `-hover` / `-light` / `-border` | 오류/삭제(Red) |
| `--color-warning` / `-hover` / `-light` / `-border` | 경고(Orange) |
| `--color-success` / `-hover` / `-light` / `-border` | 성공/정상(Green) |
| `--color-star` | 즐겨찾기 별 색상 |
| `--color-overlay` / `-strong` | 모달/패널 백드롭 |
| `--color-header-bg` | 반투명 헤더 배경(랜딩) |
| `--shadow-sm` / `-md` | 그림자(다크에서는 더 짙은 검정 기반으로 재정의) |

**새 색상을 추가할 때는 `:root`(라이트 기본값)와 `[data-theme='dark']`(다크 오버라이드)
두 곳 모두에 값을 추가해야 한다.**

### 예외 — 브랜드 고정색

아래는 다크 모드에서도 원래 브랜드 색상을 그대로 유지한다(토큰화하지 않음).

- 카카오 로그인 버튼(`#FEE500` / `#191919`) — `App.css` `.btn-kakao`
- 구글 로그인 버튼 SVG 4색 로고 — `LoginPage.tsx`, `SignupPage.tsx`
- 노벨네스트 연필 로고 SVG(`#70492E` / `#FFFFFF`) — `LoginPage.tsx`, `SignupPage.tsx`, `LandingHeader.tsx`

## select 다크 모드 화살표 — background shorthand 주의

`App.css`의 `select`에는 커스텀 dropdown 화살표를 `background-image`(data URI SVG,
`background-repeat: no-repeat`)로 그리고, `[data-theme='dark'] select`가 화살표 색상만
다시 그린 SVG로 교체하는 구조다.

`.form-group input/textarea/select, select { background: var(--color-bg-input); ... }`처럼
**shorthand `background`를 쓰면 안 된다.** `.form-group select`가 bare `select`보다
specificity가 높아, shorthand가 암묵적으로 `background-image: none` /
`background-repeat: repeat`로 초기화해버린다. 그 결과 `[data-theme='dark'] select`(동일
specificity, 소스 순서상 나중)가 `background-image`만 다시 채워 넣으면서 `repeat`가 살아남아
화살표 SVG가 박스 전체에 타일처럼 반복되는 버그가 있었다(세계관 추출 리뷰 화면의 `분류`
select에서 발견). 배경색만 지정할 때는 반드시 `background-color`를 쓴다.

## AI 섹션/챗봇 — 다크 모드 텍스트 색상 계층

`--color-ai`, `--color-primary` 두 토큰 모두 다크 모드에서 동일한 블루(`#4C8DFF`)로
정의되어 있어, AI 도구 박스(`.ai-section`)와 AI 챗봇(`AiChatPanel`)의 텍스트에 이 토큰을
그대로 쓰면 일반 텍스트까지 파랗게 보여 "파란색이 기본, 흰색이 보조"처럼 과도하게
강조되는 문제가 있었다. 라이트 모드는 두 토큰이 브랜드 브라운이라 문제가 없으므로,
다크 모드에서만 아래처럼 `[data-theme='dark']` 오버라이드로 텍스트 색상 계층을
"흰색/회색이 기본, 파란색은 포인트"로 재조정한다(라이트 모드 규칙은 그대로 둔다).

- 제목/라벨(`.ai-section-header h3`), 수치 정보(`.chat-stat-value`), AI 답변 내 인라인
  코드(`.chat-inline-code`) → `var(--color-text-primary)`(밝은 흰색)로 낮춤
- 단순 안내 문구(`.ai-usage-recharge-note`) → `var(--color-text-secondary)`(회색)로 낮춤
- 실제로 강조가 필요한 인터랙션/포인트 요소(전송 버튼, 사용자 메시지 말풍선, 제안 칩,
  입력창 포커스 링, 제목 옆 장식 아이콘 `::before`, 잔여 사용 횟수 뱃지)는 기존
  파란색 계열(`--color-primary`)을 그대로 유지한다.

새로운 AI 관련 컴포넌트를 추가할 때도 이 원칙(제목·본문·부가정보는 무채색, 실제
인터랙션/강조 요소만 포인트 컬러)을 따르고, 라이트 모드 값과 달라야 하는 항목은 반드시
`[data-theme='dark']` 오버라이드로 분리해서 추가한다.

## 다크 모드 적용 방식

```css
:root { /* 라이트 토큰 (기본값) */ }
[data-theme='dark'] { /* 다크 토큰 */ }
```

`<html>`에 `data-theme="dark"`가 붙어 있을 때만 다크 토큰이 적용된다. 이 속성은 오직
`themeStore.ts`의 `setTheme()`(사용자가 직접 토글/라디오를 조작했을 때)과
`index.html`의 FOUC 방지 스크립트(저장된 값을 복원할 때)만 설정한다. OS의
`prefers-color-scheme`은 참조하지 않는다 — 시스템 설정과는 독립적으로 동작한다.

## localStorage 저장 / FOUC 방지

- localStorage 키: `theme` (값: `'dark'`일 때만 저장. 라이트가 기본값이므로 라이트를
  선택하면 `'light'`를 그대로 저장해도 되지만, 스크립트/스토어 모두 "키가 없거나 dark가
  아니면 라이트"로 취급한다)
- `frontend/index.html`의 `<body>` 최상단, `#root`보다 먼저 실행되는 인라인 스크립트가
  React 번들 로드 전에 `localStorage.getItem('theme') === 'dark'`인지 확인해
  `data-theme="dark"`를 즉시 반영한다. 이 스크립트의 키 이름은 `themeStore.ts`의
  `STORAGE_KEY`와 반드시 일치해야 한다.
- `themeStore.ts`는 모듈 로드 시 동일한 로직으로 초기 상태를 맞춘다.

## 리렌더링

`useSyncExternalStore` + 모듈 스토어 패턴은 `aiUsageStore`에서 이미 검증된 방식으로,
테마를 구독하는 컴포넌트(`ThemeToggle`)만 리렌더링되고 나머지 컴포넌트 트리는
영향받지 않는다.

## 랜딩 페이지 스크린샷 — 테마별 자동 전환

랜딩 페이지(Hero + 핵심 기능 섹션)의 서비스 캡처는 라이트/다크 2종을 준비해 두고,
현재 테마에 맞는 한 장만 표시한다. 랜딩 전용 테마 상태는 두지 않고 위의 `themeStore`를 그대로 구독한다.

### 파일 위치 / 이름 규칙

- 위치: `frontend/public/landing/` (Vite `public` 폴더라 빌드 시 `dist/landing/`으로 그대로 복사되고,
  개발·배포 환경 모두 `/landing/<파일명>` 절대 경로로 접근한다)
- 이름: `{기능}-light.png` / `{기능}-dark.png` — 두 파일은 반드시 쌍으로 존재해야 한다.

| 기능 | 파일 이름(기능 부분) | 사용 위치 |
|---|---|---|
| 작품 상세 페이지 | `main` | `HeroSection.tsx` |
| 회차 작성 | `episode-write` | `FeatureShowcaseSection.tsx` 1행 |
| 회차 요약 | `episode-summary` | 2행 |
| 등장인물 추출 + 관리 | `character-extract`, `character-info` | 3행(별도 프레임 2개를 위아래로 배치) |
| 세계관 설정 추출 + 관리 | `world-extract`, `world-info` | 4행(별도 프레임 2개를 위아래로 배치) |
| 설정 충돌 감지 | `conflict` | 5행 |
| 작품 정보 챗봇 | `chatbot` | 6행 |

### 동작 방식

- `components/landing/LandingImage.tsx`가 `lightSrc`/`darkSrc`를 받아 `useTheme()` 값으로 하나만 `<img>`에
  넣는다. 렌더링되지 않은 반대 테마 이미지는 다운로드되지 않는다.
- `themeStore`는 모듈 로드 시 localStorage를 읽어 두므로 첫 렌더부터 올바른 테마 이미지가 선택된다
  (반대 테마 이미지가 잠깐 보이는 현상 없음). 토글 시 `src`만 바뀌므로 새로고침 없이 즉시 전환된다.
- 같은 기능이라도 라이트/다크 캡처의 픽셀 크기가 조금씩 다르다. 그래서 `width`/`height`(박스 기준 크기)로
  `aspect-ratio`를 고정하고 `object-fit: contain`으로 넣어 테마 전환 시 레이아웃이 흔들리지 않게 한다.
  박스 기준 크기는 두 캡처 중 **세로로 더 긴 쪽**의 픽셀 크기를 쓴다(다른 쪽은 위아래 몇 px 여백만 생기고 잘리지 않음).
- 로드 실패 시 깨진 이미지 아이콘 대신 "이미지를 준비 중입니다" 안내 박스를 같은 비율로 보여준다.
- Hero 이미지(LCP)는 `eager` + `fetchPriority="high"`, 나머지는 `loading="lazy"`.
  `frontend/index.html` `<head>`의 인라인 스크립트가 저장된 테마에 맞는 Hero 이미지(`main-{theme}.png`)
  한 장만 프리로드한다.

### 레이아웃 규칙

- 프레임 최대 너비는 기본적으로 캡처 원본 너비로 제한한다(작은 캡처가 확대되어 글자가 흐려지는 것 방지).
  `FEATURES`의 `frameMaxWidth`로 개별 지정할 수 있다(설정 충돌 캡처는 세로가 길어 400px로 지정).
- 세로형 캡처(높이 > 너비)는 `.landing-browser-frame-img-tall`이 자동으로 붙어 기존 최대 높이(560px) 제한을 해제한다.
- 한 기능에 캡처가 여러 장이면 캡처마다 별도 프레임(박스)으로 감싸 위아래로 20px 간격을 두고 배치한다. 같은 기능의 프레임 너비는 동일하게 맞춘다.

### 이미지 추가/교체 시

1. 라이트/다크 캡처를 같은 영역·비슷한 크기로 찍어 `{기능}-light.png` / `{기능}-dark.png`로 저장한다.
2. 두 캡처 중 세로로 더 긴 쪽의 픽셀 크기를 `FEATURES`(또는 `HeroSection`)의 `width`/`height`에 반영한다.
3. Hero 이미지 이름을 바꾸면 `frontend/index.html`의 프리로드 스크립트 경로도 함께 바꾼다.
4. 원본보다 크게 표시되지 않도록, 캡처는 실제 표시 크기 이상(가급적 900px 이상 너비)으로 준비한다.
