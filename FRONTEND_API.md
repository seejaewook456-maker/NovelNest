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

## AI 세계관 추출(WorldSettingExtraction) API — 프론트 연동

### AI 세계관 후보 추출

```
POST /api/episodes/{episodeId}/world-setting-extraction
Authorization: Bearer {accessToken}
```

**설명**: AI가 회차 본문을 분석하여 세계관/설정 후보를 반환합니다. DB에 저장하지 않으며, 반환된 후보를 프론트엔드에서 1개씩 검토 후 저장합니다.

**Response (200)**
```json
{
  "message": "세계관 추출 성공",
  "data": {
    "episodeTitle": "1화 - 시작",
    "totalCount": 2,
    "candidates": [
      {
        "category": "ITEM",
        "title": "아카식의 서",
        "content": "계승자만 펼칠 수 있는 금서",
        "evidence": "아카식의 서는 계승자만 펼칠 수 있는 금서였다.",
        "isExistingSetting": false,
        "matchedWorldSettingId": null,
        "existingWorldSetting": null,
        "newInsights": null
      },
      {
        "category": "MAGIC",
        "title": "봉인 마법",
        "content": "봉인 마법은 계약자 혈통만 사용 가능하며, 발동 시 손목에 낙인이 남는다.",
        "evidence": "그의 손목에 붉은 낙인이 새겨졌다.",
        "isExistingSetting": true,
        "matchedWorldSettingId": 3,
        "existingWorldSetting": {
          "id": 3,
          "novelId": 1,
          "category": "MAGIC",
          "title": "봉인 마법",
          "content": "봉인 마법은 계약자 혈통만 사용 가능하다.",
          "createdAt": "...",
          "updatedAt": "..."
        },
        "newInsights": {
          "content": ["발동 시 손목에 붉은 낙인이 남는다"]
        }
      }
    ]
  }
}
```

**저장 방식** (DB 저장은 사용자 검토 후 기존 API로):
- 신규 설정: `POST /api/novels/{novelId}/world-settings` with `{ category, title, content }`
- 기존 설정 보강: `PATCH /api/world-settings/{matchedWorldSettingId}` with `{ category, title, content }`

### 프론트 연동 흐름

```
EpisodeDetailPage
→ [AI 세계관 추출] 버튼 클릭
→ POST /api/episodes/{episodeId}/world-setting-extraction
→ navigate('/episodes/{episodeId}/world-setting-review', { state: { candidates, novelId, episodeId, episodeTitle } })

WorldSettingReviewPage (1/N ~ N/N)
→ 신규(isExistingSetting=false): category/title/content 수정 → POST /api/novels/{novelId}/world-settings
→ 기존(isExistingSetting=true): 기존 내용 + newInsights 표시 → PATCH /api/world-settings/{matchedWorldSettingId}
→ 완료: 신규 저장 N건 / 기존 보강 N건 / 건너뜀 N건 통계 표시
```

### 관련 파일

| 파일 | 역할 |
|------|------|
| `src/api/worldSettingExtractionApi.ts` | 추출 API 호출 |
| `src/types/worldSettingExtraction.ts` | 추출 결과 타입 정의 |
| `src/pages/WorldSettingReviewPage.tsx` | 검토 화면 |
| `src/api/worldSettingApi.ts` | 저장/수정 (기존 파일 재사용) |
| `src/types/worldsetting.ts` | WorldSettingCategory enum, CATEGORY_LABELS (기존 파일 재사용) |

---

## AI 설정 충돌 탐지(ConflictDetection) API

### 설정 충돌 탐지

```
POST /api/episodes/{episodeId}/conflict-detection
Authorization: Bearer {accessToken}
```

**설명**: 현재 회차 본문을 등장인물 정보, 세계관 설정, 이전 회차 요약과 비교하여 충돌 가능성을 반환합니다. DB에 저장하지 않으며 읽기 전용 분석입니다. AI가 자동 수정하거나 저장하지 않습니다.

**Request Body**: 없음 (episodeId는 경로 변수)

**Response (200)**
```json
{
  "message": "충돌 탐지 완료",
  "data": {
    "episodeTitle": "12화 - 검은 달의 밤",
    "conflictCount": 2,
    "conflicts": [
      {
        "type": "WORLD_SETTING_CONFLICT",
        "severity": "HIGH",
        "title": "에테르의 서 사용 조건 충돌",
        "existingInfo": "에테르의 서는 계승자만 사용할 수 있다.",
        "currentEpisodeInfo": "루시안이 에테르의 서를 사용한 것으로 묘사된다.",
        "description": "기존 세계관 설정과 현재 회차 내용이 충돌할 가능성이 있습니다.",
        "suggestion": "루시안이 계승자인지, 혹은 예외적으로 사용할 수 있는 조건이 있는지 설명을 추가하는 것을 검토하세요."
      },
      {
        "type": "TIMELINE_CONFLICT",
        "severity": "HIGH",
        "title": "사망 인물 재등장",
        "existingInfo": "3화 요약: 박진호가 전투 중 사망하였다.",
        "currentEpisodeInfo": "박진호가 이번 회차에서 대화 장면에 등장한다.",
        "description": "이전 회차 요약 기준 사망한 인물이 현재 회차에 등장하여 시간선 충돌 가능성이 있습니다.",
        "suggestion": "의도된 회상 장면이거나 오류인지 검토하세요. 오류라면 인물명 수정이 필요합니다."
      }
    ]
  }
}
```

**충돌이 없을 때 Response**
```json
{
  "message": "충돌 탐지 완료",
  "data": {
    "episodeTitle": "3화 - 첫 만남",
    "conflictCount": 0,
    "conflicts": []
  }
}
```

**충돌 유형(type)**

| 값 | 설명 |
|---|---|
| `CHARACTER_CONFLICT` | 인물 기본 정보 충돌 (나이, 이름 등) |
| `PERSONALITY_CONFLICT` | 성격/말투/행동 패턴 충돌 |
| `RELATIONSHIP_CONFLICT` | 인물 관계 충돌 |
| `WORLD_SETTING_CONFLICT` | 세계관 설정 충돌 |
| `ABILITY_CONFLICT` | 능력/마법/아이템 사용 조건 충돌 |
| `TIMELINE_CONFLICT` | 시간선/사건 순서 충돌 |

**심각도(severity)**

| 값 | 기준 |
|---|---|
| `HIGH` | 사망/생존 충돌, 나이 등 명확한 정보 충돌, 세계관 규칙 위반 |
| `MEDIUM` | 성격 급변, 관계 설정 애매, 능력 조건 불명확 |
| `LOW` | 추가 설명 권장, 독자 혼동 우려, 가벼운 설정 보강 필요 |

### 프론트 연동 흐름

```
EpisodeDetailPage
→ [AI 충돌 탐지] 버튼 클릭
→ POST /api/episodes/{episodeId}/conflict-detection
→ ConflictDetectionResultPage로 navigate (state로 conflicts 전달)
  또는 모달/인라인 표시

ConflictDetectionResultPage
→ 충돌 목록을 severity(HIGH → MEDIUM → LOW) 순으로 표시
→ 각 충돌 카드: type 배지, severity 배지, title, existingInfo, currentEpisodeInfo, description, suggestion
→ 충돌 없으면 "충돌이 발견되지 않았습니다" 빈 상태 표시
→ 결과는 저장되지 않음 — 작가가 직접 수정 후 Character/WorldSetting 편집 API 호출
```

### 관련 파일

| 파일 | 역할 |
|------|------|
| `src/api/conflictDetectionApi.ts` | 탐지 API 호출 |
| `src/types/conflictDetection.ts` | ConflictResult 타입 + CONFLICT_TYPE_LABELS |
| `src/pages/EpisodeDetailPage.tsx` | 충돌 감지 섹션 + ConflictSummaryBar + ConflictCard 컴포넌트 |

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
