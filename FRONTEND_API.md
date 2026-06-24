# Frontend API 명세서

React 프론트엔드에서 백엔드 API를 호출하는 방법을 정리한 문서입니다.

---

## 공통 사항

### 백엔드 주소

| 환경 | 주소 |
|---|---|
| 로컬 백엔드 | `http://localhost:8080` |
| 로컬 프론트 | `http://localhost:5173` |

> `vite.config.ts`의 proxy 설정으로 `/api/*` 요청은 자동으로 `localhost:8080`으로 전달됩니다.
> 브라우저에서는 `localhost:5173/api/...` 형태로 호출하면 됩니다.

### 공통 응답 구조 (ApiResponse)

```json
{
  "message": "로그인 성공",
  "data": { ... }
}
```

- `message`: 결과 메시지 (항상 포함)
- `data`: 응답 데이터 (없는 경우 JSON에서 제외됨)

### 에러 응답

```json
{
  "message": "이메일 또는 비밀번호가 올바르지 않습니다."
}
```

HTTP 상태 코드: `400` (잘못된 요청), `403` (권한 없음)

---

## JWT 토큰 관리

### 저장

로그인 성공 시 `localStorage`에 저장합니다.

```typescript
localStorage.setItem('accessToken', token);
```

### 사용

인증이 필요한 모든 요청에 헤더로 포함합니다.

```typescript
headers: {
  'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
}
```

### 삭제 (로그아웃)

```typescript
localStorage.removeItem('accessToken');
```

---

## 인증 API

### 로그인

```
POST /api/users/login
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response (200)**
```json
{
  "message": "로그인 성공",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

**React 연동**
```typescript
const accessToken = await login({ email, password });
saveToken(accessToken);
navigate('/novels');
```

---

### 회원가입

```
POST /api/users/signup
```

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "홍길동"
}
```

**Response (201)**
```json
{
  "message": "회원가입 성공"
}
```

> `data` 필드 없음 — 회원가입 성공 후 로그인 페이지로 이동합니다.

---

## 작품(Novel) API

> 모든 작품 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

### 내 작품 목록 조회

```
GET /api/novels
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "message": "내 작품 목록 조회 성공",
  "data": [
    {
      "id": 1,
      "userId": 1,
      "title": "검은 달의 기사",
      "genre": "판타지",
      "description": "마법이 사라진 세계의 이야기",
      "createdAt": "2026-06-24T10:00:00",
      "updatedAt": "2026-06-24T10:00:00"
    }
  ]
}
```

---

### 작품 생성

```
POST /api/novels
Authorization: Bearer {accessToken}
```

**Request**
```json
{
  "title": "검은 달의 기사",
  "genre": "판타지",
  "description": "마법이 사라진 세계의 이야기"
}
```

> `description`은 선택 입력입니다.

**Response (201)**
```json
{
  "message": "작품 생성 성공",
  "data": {
    "id": 1,
    "userId": 1,
    "title": "검은 달의 기사",
    "genre": "판타지",
    "description": "마법이 사라진 세계의 이야기",
    "createdAt": "2026-06-24T10:00:00",
    "updatedAt": "2026-06-24T10:00:00"
  }
}
```

---

### 작품 상세 조회

```
GET /api/novels/{novelId}
Authorization: Bearer {accessToken}
```

**Response (200)**: 작품 생성 응답과 동일

---

### 작품 수정

```
PUT /api/novels/{novelId}
Authorization: Bearer {accessToken}
```

**Request**: 작품 생성 요청과 동일 (title, genre, description)

**Response (200)**: 수정된 작품 정보 반환

---

### 작품 삭제

```
DELETE /api/novels/{novelId}
Authorization: Bearer {accessToken}
```

**Response (200)**
```json
{
  "message": "작품 삭제 성공"
}
```

---

## 회차(Episode) API

> 모든 회차 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

### 회차 목록 조회

```
GET /api/episodes?novelId={novelId}
```

**Response (200)**
```json
{
  "message": "회차 목록 조회 성공",
  "data": [
    { "id": 1, "novelId": 1, "title": "시작", "episodeNumber": 1, "content": "...", "createdAt": "...", "updatedAt": "..." }
  ]
}
```

---

### 회차 생성

```
POST /api/episodes?novelId={novelId}
```

**Request**
```json
{ "title": "시작", "episodeNumber": 1, "content": "본문 내용" }
```

**Response (201)**: 생성된 회차 정보 반환

---

### 회차 상세 조회

```
GET /api/episodes/{episodeId}
```

**Response (200)**: 회차 상세 정보 반환

---

### 회차 수정 (전체 교체)

```
PATCH /api/episodes/{episodeId}
```

**Request**: `{ title, episodeNumber, content }` — 3개 필드 모두 필수

**Response (200)**: 수정된 회차 정보 반환

---

### 회차 삭제

```
DELETE /api/episodes/{episodeId}
```

**Response (200)**: `{ "message": "회차 삭제 성공" }`

---

## 등장인물(Character) API

> 모든 인물 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

### 인물 목록 조회

```
GET /api/characters?novelId={novelId}
```

**Response (200)**
```json
{
  "message": "등장인물 목록 조회 성공",
  "data": [
    { "id": 1, "novelId": 1, "name": "홍길동", "role": "주인공", "age": "25세", "personality": "용감함", "speechStyle": "반말", "description": "설명", "createdAt": "...", "updatedAt": "..." }
  ]
}
```

---

### 인물 생성

```
POST /api/characters?novelId={novelId}
```

**Request**
```json
{ "name": "홍길동", "role": "주인공", "age": "25세", "personality": "용감함", "speechStyle": "반말", "description": "설명" }
```

> `name`만 필수, 나머지는 선택

**Response (201)**: 생성된 인물 정보 반환

---

### 인물 수정

```
PATCH /api/characters/{characterId}
```

**Request**: 생성 요청과 동일 구조

**Response (200)**: 수정된 인물 정보 반환

---

### 인물 삭제

```
DELETE /api/characters/{characterId}
```

**Response (200)**: `{ "message": "등장인물 삭제 성공" }`

---

## 세계관(WorldSetting) API

> 모든 세계관 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

### 카테고리 목록

`COUNTRY(국가)`, `RACE(종족)`, `MAGIC(마법)`, `ORGANIZATION(조직)`, `PLACE(장소)`, `EVENT(사건)`, `ITEM(아이템)`, `RULE(규칙)`, `ETC(기타)`

### 세계관 목록 조회

```
GET /api/world-settings?novelId={novelId}
```

**Response (200)**
```json
{
  "message": "세계관 설정 목록 조회 성공",
  "data": [
    { "id": 1, "novelId": 1, "category": "MAGIC", "title": "마법 체계", "content": "설명", "createdAt": "...", "updatedAt": "..." }
  ]
}
```

---

### 세계관 설정 생성

```
POST /api/world-settings?novelId={novelId}
```

**Request**
```json
{ "category": "MAGIC", "title": "마법 체계", "content": "설명 내용" }
```

**Response (201)**: 생성된 설정 정보 반환

---

### 세계관 설정 수정

```
PATCH /api/world-settings/{worldSettingId}
```

**Request**: 생성 요청과 동일 구조

**Response (200)**: 수정된 설정 정보 반환

---

### 세계관 설정 삭제

```
DELETE /api/world-settings/{worldSettingId}
```

**Response (200)**: `{ "message": "세계관 설정 삭제 성공" }`

---

## 회차-등장인물 연결(EpisodeCharacter) API

> 모든 API는 `Authorization: Bearer {accessToken}` 헤더가 필요합니다.

### 회차-인물 연결 생성

```
POST /api/episodes/{episodeId}/characters/{characterId}
Authorization: Bearer {accessToken}
```

**설명**: AI 추출 검토 후 저장 시 자동 호출됩니다. 이미 연결된 경우 조용히 무시됩니다 (멱등).

**Response (201)**
```json
{
  "message": "회차-등장인물 연결 성공"
}
```

---

### 회차별 추출 인물 목록 조회

```
GET /api/episodes/{episodeId}/characters
Authorization: Bearer {accessToken}
```

**설명**: 해당 회차에서 AI 추출 후 저장된 인물 목록을 반환합니다. 작품 전체 인물이 아닌 이 회차에서 저장한 인물만 반환됩니다.

**Response (200)**
```json
{
  "message": "회차별 등장인물 조회 성공",
  "data": [
    {
      "id": 1,
      "novelId": 1,
      "name": "김하준",
      "role": "주인공",
      "age": 25,
      "personality": "용감하고 정의감이 강함",
      "speechStyle": "반말",
      "description": "설명",
      "createdAt": "2026-06-24T10:00:00",
      "updatedAt": "2026-06-24T10:00:00"
    }
  ]
}
```

---

## AI 등장인물 추출(CharacterExtraction) API

### AI 등장인물 후보 추출

```
POST /api/episodes/{episodeId}/character-extraction
Authorization: Bearer {accessToken}
```

**설명**: AI가 회차 본문을 분석하여 등장인물 후보를 반환합니다. DB에 저장하지 않으며, 반환된 후보를 프론트엔드에서 1명씩 검토 후 저장합니다.

**Response (200)**
```json
{
  "message": "등장인물 후보 추출 성공",
  "data": {
    "episodeTitle": "1화 - 시작",
    "totalCount": 2,
    "candidates": [
      {
        "name": "김하준",
        "role": "주인공",
        "age": 25,
        "personality": "용감함",
        "speechStyle": "반말",
        "description": "설명",
        "evidence": "근거 장면",
        "isExistingCharacter": false,
        "matchedCharacterId": null,
        "newInsights": null,
        "existingCharacter": null
      }
    ]
  }
}
```

---

## AI 회차 요약(EpisodeSummary) API

### AI 회차 요약 생성/재생성

```
POST /api/episodes/{episodeId}/summary
Authorization: Bearer {accessToken}
```

**Response (201)**
```json
{
  "message": "회차 요약 생성 성공",
  "data": {
    "id": 1,
    "episodeId": 1,
    "summary": "이 회차에서 ...",
    "createdAt": "...",
    "updatedAt": "..."
  }
}
```

---

### 회차 요약 조회

```
GET /api/episodes/{episodeId}/summary
Authorization: Bearer {accessToken}
```

**Response (200)**: 요약 생성 응답과 동일 / 요약 없을 경우 400

---

## 페이지 라우팅 구조

| 경로 | 페이지 |
|---|---|
| `/login` | 로그인 |
| `/signup` | 회원가입 |
| `/novels` | 작품 목록 |
| `/novels/new` | 작품 생성 |
| `/novels/:novelId` | 작품 상세 |
| `/novels/:novelId/episodes` | 회차 목록 |
| `/novels/:novelId/episodes/new` | 회차 생성 |
| `/episodes/:episodeId` | 회차 상세/수정/삭제 |
| `/novels/:novelId/characters` | 등장인물 관리 (인라인 CRUD) |
| `/novels/:novelId/world-settings` | 세계관 관리 (인라인 CRUD) |

---

## 인증 가드 (PrivateRoute)

로그인하지 않은 사용자가 `/novels` 등 보호된 페이지에 접근하면 `/login`으로 자동 리다이렉트됩니다.

```typescript
// router/index.tsx
function PrivateRoute({ children }) {
  if (!isLoggedIn()) return <Navigate to="/login" replace />;
  return <>{children}</>;
}
```

---

## 실행 방법

### 백엔드 실행
```bash
./gradlew bootRun
# http://localhost:8080
```

### 프론트엔드 실행
```bash
cd frontend
npm run dev
# http://localhost:5173
```

---

## 로그인 테스트 순서

1. `http://localhost:5173` 접속 → `/login` 자동 이동
2. 계정이 없으면 "회원가입" 링크 클릭 → 회원가입
3. 로그인 성공 → `/novels` 이동
4. 브라우저 개발자 도구 → Application → Local Storage → `accessToken` 확인
5. "새 작품" 버튼 → 작품 생성 → 목록 확인
6. "로그아웃" 버튼 → `accessToken` 삭제 확인 → `/login` 이동

---

## JWT 저장 확인 방법

1. Chrome 개발자 도구 열기 (`F12`)
2. `Application` 탭 → `Local Storage` → `http://localhost:5173`
3. `accessToken` 키에 JWT 토큰이 저장되어 있으면 정상
