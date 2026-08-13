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
