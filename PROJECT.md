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

## 아직 구현되지 않음

### Frontend
* 회차 요약 UI

### Backend (AI 기능)
* 등장인물 AI 추출 (CharacterExtraction) — 후보 생성, DB 저장 없음
  - POST /api/episodes/{episodeId}/character-extraction
  - 신규/기존 인물 구분, newInsights(새 발견 정보) 반환

### AI 기능 (미구현)
* 설정 충돌 탐지
* 문체 분석

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
