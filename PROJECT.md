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

### Frontend (MVP 1차)
* React 19 + TypeScript + Vite (frontend/ 폴더)
* 로그인 페이지 (JWT localStorage 저장)
* 회원가입 페이지
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
