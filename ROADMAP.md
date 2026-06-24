# ROADMAP

## 완료

- [x] 회원가입 API
- [x] 로그인 API (JWT accessToken 발급)
- [x] JWT 인증 필터 및 내 정보 조회 API
- [x] 작품(Novel) CRUD API
- [x] 회차(Episode) CRUD API
- [x] 등장인물(Character) CRUD API
- [x] 세계관(WorldSetting) CRUD API

---

## MVP 구현 순서

### Phase 1 — 콘텐츠 뼈대 (완료)

- [x] Novel (작품) CRUD
- [x] Episode (회차) CRUD
- [x] Character (등장인물) CRUD
- [x] WorldSetting (세계관) CRUD

### Phase 2 — AI 기능 (다음 작업)

- [ ] OpenAI API 연동
- [ ] 문체 분석 (반복 표현, 문장 길이, 시점 혼동, 대사 비율)
- [ ] 등장인물 자동 추출 (회차 원고 → 이름/성격/역할)
- [ ] 회차 요약 생성 (EpisodeSummary)
- [ ] 설정 충돌 탐지 (모순 감지)

### Phase 3 — RAG 도입

- [ ] 벡터 DB 연동
- [ ] 작품/회차/인물/세계관 임베딩 수집
- [ ] AI 분석 시 컨텍스트 참조 구조 (현재 회차 + 이전 요약 + 인물 + 세계관)

---

## API 목록

### 인증 (`/api/users`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/users/signup` | 회원가입 |
| POST | `/api/users/login` | 로그인 |
| GET | `/api/users/me` | 내 정보 조회 |

### 작품 (`/api/novels`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/novels` | 작품 생성 |
| GET | `/api/novels` | 내 작품 목록 조회 |
| GET | `/api/novels/{novelId}` | 작품 상세 조회 |
| PUT | `/api/novels/{novelId}` | 작품 수정 |
| DELETE | `/api/novels/{novelId}` | 작품 삭제 |

### 회차 (`/api/novels/{novelId}/episodes`, `/api/episodes`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/novels/{novelId}/episodes` | 회차 생성 |
| GET | `/api/novels/{novelId}/episodes` | 회차 목록 조회 |
| GET | `/api/episodes/{episodeId}` | 회차 상세 조회 |
| PATCH | `/api/episodes/{episodeId}` | 회차 수정 |
| DELETE | `/api/episodes/{episodeId}` | 회차 삭제 |

### 등장인물 (`/api/novels/{novelId}/characters`, `/api/characters`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/novels/{novelId}/characters` | 등장인물 생성 |
| GET | `/api/novels/{novelId}/characters` | 등장인물 목록 조회 |
| GET | `/api/characters/{characterId}` | 등장인물 상세 조회 |
| PATCH | `/api/characters/{characterId}` | 등장인물 수정 |
| DELETE | `/api/characters/{characterId}` | 등장인물 삭제 |

### 세계관 (`/api/novels/{novelId}/world-settings`, `/api/world-settings`)

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/novels/{novelId}/world-settings` | 세계관 설정 생성 |
| GET | `/api/novels/{novelId}/world-settings` | 세계관 설정 목록 조회 |
| GET | `/api/world-settings/{worldSettingId}` | 세계관 설정 상세 조회 |
| PATCH | `/api/world-settings/{worldSettingId}` | 세계관 설정 수정 |
| DELETE | `/api/world-settings/{worldSettingId}` | 세계관 설정 삭제 |
