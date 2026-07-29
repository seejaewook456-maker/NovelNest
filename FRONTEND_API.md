1# Frontend API 명세서

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

### Google OAuth 로그인 흐름

일반 API 호출이 아닌 **브라우저 리다이렉트** 방식입니다.

```
1. 사용자가 "Google로 로그인" 버튼 클릭
   → <a href="http://localhost:8080/oauth2/authorization/google">

2. 백엔드(Spring Security)가 Google 인증 페이지로 리다이렉트

3. 사용자가 Google 계정 선택 및 동의

4. Google이 백엔드 콜백 URL로 리다이렉트
   → GET http://localhost:8080/login/oauth2/code/google?code=...

5. 백엔드가 JWT 발급 후 프론트로 리다이렉트
   → 성공: http://localhost:5173/oauth2/callback?token=JWT_TOKEN
   → 실패: http://localhost:5173/login?error=에러메시지(URL인코딩)

6. OAuth2CallbackPage: token 파라미터를 localStorage에 저장 → /novels 이동
   LoginPage: error 파라미터를 읽어 에러 메시지 표시
```

**주의사항**
- `<a>` 태그 직접 이동 방식 (fetch/axios 사용 불가 — CORS 및 리다이렉트 흐름 때문)
- `http://localhost:8080/oauth2/authorization/google` — Vite proxy를 거치지 않음
- LOCAL 계정과 동일한 이메일로 Google 로그인 시 에러 (`/login?error=...`)

---

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

## AI 사용량(AiUsage) API

사용자별 AI 기능 하루 사용량 제한과 잔여 횟수 조회. 정책/제한값/일일 초기화 방식 등 전체 내용은
`docs/AI_USAGE_LIMIT.md`를 참고. 이 문서에는 프론트 연동에 필요한 부분만 요약한다.

### 오늘의 AI 사용량 조회

```
GET /api/ai/usage/daily
Authorization: Bearer {accessToken}
```

**설명**: 이 조회 자체는 사용 횟수에 포함되지 않는다.

**Response (200)**
```json
{
  "message": "AI 사용량 조회 성공",
  "data": {
    "usageDate": "2026-07-29",
    "timezone": "Asia/Seoul",
    "nextResetAt": "2026-07-30T00:00:00+09:00",
    "summary": { "used": 1, "remaining": 19, "limit": 20 },
    "conflict": { "used": 1, "remaining": 14, "limit": 15 },
    "character": { "used": 1, "remaining": 14, "limit": 15 },
    "worldview": { "used": 1, "remaining": 14, "limit": 15 },
    "chat": { "used": 2, "remaining": 18, "limit": 20 }
  }
}
```

전체 합계 제한은 없다 — 각 기능의 `remaining`은 그 기능 자체의 잔여 횟수 그대로다. 프론트엔드는 이
값을 그대로 표시하면 되고, 별도로 다시 계산하지 않는다.

### AI 기능 실행 시 제한 초과 응답 (429)

회차 요약/설정 충돌 감지/등장인물 추출/세계관 추출/AI 챗봇 API를 호출했을 때, 해당 기능의 오늘 사용
가능한 횟수를 모두 소진한 상태면 아래처럼 `429 Too Many Requests`가 반환된다(OpenAI 호출 자체가
일어나지 않으므로 과금도 발생하지 않는다).

```json
{
  "success": false,
  "code": "AI_DAILY_FEATURE_LIMIT_EXCEEDED",
  "message": "오늘 사용할 수 있는 회차 요약 횟수를 모두 사용했습니다. AI 도구 이용권은 매일 00:00에 충전됩니다."
}
```

프론트엔드는 이 `message`를 그대로 사용자에게 보여준다(`fetchWithAuth`가 던지는 `ApiError.message`).

### 프론트 연동 흐름

```
useAiDailyUsage() (여러 컴포넌트가 공유하는 훅)
→ 최초 마운트 시 GET /api/ai/usage/daily 1회 조회 (aiUsageStore에 캐시)
→ AI 실행 성공/실패 직후 refetch() 호출 → store가 갱신되며 이를 구독 중인 모든 컴포넌트가 즉시 리렌더

EpisodeDetailPage (AI 도구 4종)
→ 각 섹션에 <AiUsageHint detail={aiUsage?.summary|conflict|character|worldview} .../> 표시
→ remaining <= 0이면 실행 버튼 disabled

AiChatPanel (작품 상세/회차 작성/회차 수정 3개 페이지 공유)
→ <AiUsageHint detail={aiUsage?.chat} .../> 표시
→ remaining <= 0이면 입력창/전송 버튼 disabled
```

### 관련 파일

| 파일 | 역할 |
|------|------|
| `src/types/aiUsage.ts` | `AiDailyUsage`/`AiUsageDetail` 타입 |
| `src/api/aiUsageApi.ts` | `GET /api/ai/usage/daily` 호출 |
| `src/state/aiUsageStore.ts` | 여러 컴포넌트가 공유하는 조회 결과 캐시(pub-sub) — `sessionExpired.ts`와 동일한 패턴 |
| `src/hooks/useAiDailyUsage.ts` | 공통 훅(`usage`/`loading`/`error`/`refetch`) |
| `src/components/AiUsageHint.tsx` | "오늘 남은 횟수" 표시 공통 컴포넌트 (로딩/에러/소진 상태 포함) |
| `src/pages/EpisodeDetailPage.tsx` | AI 도구 4종 섹션에 훅/컴포넌트 적용 |
| `src/components/AiChatPanel.tsx` | 챗봇 입력 제한에 훅/컴포넌트 적용 |

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

## GA4 애널리틱스 연동

### 구조

| 파일 | 역할 |
|---|---|
| `src/lib/analytics.ts` | GA4 연동 모듈 — `initializeAnalytics()` / `trackPageView()` / `trackEvent()` 제공. `ReactGA.event`/`ReactGA.send`를 직접 호출하는 곳은 이 파일뿐이다 |
| `src/constants/analyticsEvents.ts` | 이벤트명 상수(`ANALYTICS_EVENTS`), 인증 수단 상수(`AUTH_METHOD`) — 오타 방지용, 이벤트 전송 시 항상 이 상수를 사용 |
| `src/utils/oauthProvider.ts` | Google/카카오 로그인 버튼 클릭 시점의 provider를 세션에 잠깐 저장해, OAuth 콜백에서 `login` 이벤트의 `method`를 채우는 용도 |
| `src/router/index.tsx` | `router.subscribe`로 페이지 이동을 감지해 `page_view`를 전송 (하단 참고) |

### 초기화

`initializeAnalytics()`는 `VITE_GA_MEASUREMENT_ID` 환경변수가 설정된 경우에만 GA4를 초기화하고, 없으면 `console.warn`만 남기고 아무 것도 하지 않는다(로컬 개발 환경 기본값). 자동 `page_view`는 끄고(`send_page_view: false`) 아래 방식으로 React Router 쪽에서 직접 전송한다.

`router/index.tsx` 최상단에서 호출한다 — `main.tsx`가 아니라 여기서 호출하는 이유는, ES 모듈은 import된 모듈의 최상위 코드를 import한 쪽의 코드보다 먼저 실행하기 때문에 `main.tsx`에서 호출하면 라우터 모듈의 최초 `page_view` 전송이 초기화보다 먼저 일어날 수 있기 때문이다. `initializeAnalytics()`는 중복 호출해도 한 번만 초기화된다.

### 페이지 추적 (page_view)

이 프로젝트는 `createBrowserRouter`(데이터 라우터) 구조라 `<BrowserRouter>` 하위에 추적용 컴포넌트를 둘 root 레이아웃이 없다. 대신 `router.subscribe`로 위치 변화를 구독해서, 라우트 구성을 바꾸지 않고 모든 경로의 이동을 추적한다.

- **동적 라우트 정규화**: `/novels/123` → `/novels/:novelId`처럼, 실제 pathname 대신 매칭된 라우트의 `route.path` 패턴을 그대로 사용한다. 별도 정규식 없이 라우터 설정과 항상 일치한다.
- **쿼리스트링(search)**: 전송하지 않는다. `/oauth2/callback?token=...&refreshToken=...`처럼 민감한 값이 쿼리에 실리는 라우트가 있어, page_path에는 pathname만 사용한다.
- **중복 방지**: `location.key`(history 엔트리별 고유값)로 실제 이동 여부를 판단한다. 정규화된 경로 문자열이 아니라 key로 비교하므로, `/novels/1` → `/novels/2`처럼 같은 라우트 패턴이라도 실제 이동이면 각각 정상 집계되고, StrictMode 등으로 인한 동일 상태 재통지는 무시된다.

### 이벤트 (trackEvent)

`trackEvent(ANALYTICS_EVENTS.XXX, params?)` 형태로 호출하며, 모두 **API 성공 이후**(버튼 클릭 시점이 아님) 호출부에서 직접 전송한다. 실패한 작업은 이벤트를 전송하지 않는다.

| 이벤트 | 호출 위치 | 비고 |
|---|---|---|
| `sign_up` | `SignupPage` (이메일 가입 성공) | `method: 'email'` |
| `login` | `LoginPage` (이메일 로그인 성공), `OAuth2CallbackPage` (OAuth 콜백 성공) | `method: 'email' \| 'google' \| 'kakao'`. OAuth는 신규가입/기존로그인을 백엔드가 구분해 내려주지 않아 항상 `login`으로 전송 |
| `novel_create` | `NovelCreatePage` | |
| `episode_create` | `EpisodeCreatePage` | |
| `episode_update` | `EpisodeDetailPage` (수동 저장 제출 성공) | 자동저장(autosave)은 전송하지 않음 |
| `episode_copy` | `EpisodeDetailPage` (본문 복사 버튼) | |
| `ai_summary_run` | `EpisodeDetailPage` (AI 요약 생성) | |
| `ai_conflict_check_run` | `EpisodeDetailPage` (설정 충돌 감지) | |
| `ai_character_extract_run` | `EpisodeDetailPage` (AI 등장인물 추출) | |
| `ai_worldview_extract_run` | `EpisodeDetailPage` (AI 세계관 추출) | |
| `ai_chat_message_send` | `AiChatPanel` (채팅 전송 성공) | |
| `account_delete` | `NovelListPage` (회원 탈퇴 성공) | |

### 개인정보 보호

이벤트 파라미터에는 이메일, Access/Refresh Token, providerId, 사용자 ID, 소설 제목/본문, AI 질문/응답, 등장인물/세계관 정보 등 사용자 콘텐츠·개인정보를 절대 포함하지 않는다. 위 표의 이벤트는 `method` 외에는 파라미터 없이 전송된다.

### 환경변수

| 변수 | 설명 |
|---|---|
| `VITE_GA_MEASUREMENT_ID` | GA4 Measurement ID (예: `G-XXXXXXX`). 없으면 GA4 비활성화(경고 로그만 출력) |

`frontend/.env.production`에 기본값이 커밋되어 있다(다른 `VITE_*` 값과 동일한 관례). Vercel 배포 시 대시보드에 동일한 키로 등록해두면 코드 변경 없이 값을 교체할 수 있다.

---

## Microsoft Clarity 연동

### 구조

| 파일 | 역할 |
|---|---|
| `src/lib/clarity.ts` | Clarity 연동 모듈 — `initializeClarity()` 하나만 제공. Clarity 공식 추적 스크립트를 동적으로 `<head>`에 삽입하는 코드는 이 파일뿐이다 |
| `src/main.tsx` | 앱 진입점에서 `initializeClarity()`를 한 번 호출 |

### 초기화

`initializeClarity()`는 `VITE_CLARITY_PROJECT_ID` 환경변수가 있을 때만 Clarity 스크립트를 삽입한다. 없으면(로컬 개발 환경 기본값) GA4와 달리 경고조차 남기지 않고 조용히 아무 것도 하지 않는다.

- **중복 삽입 방지**: 모듈 내부 `isInitialized` 플래그 + 스크립트 태그 `id`(`ms-clarity-script`) 존재 여부를 이중으로 확인해, StrictMode 이중 실행이나 재호출에도 스크립트를 두 번 삽입하지 않는다.
- **초기화 실패 격리**: DOM 조작 전체를 `try/catch`로 감싸 Clarity 쪽 오류가 앱 렌더링에 영향을 주지 않는다.
- **호출 위치**: `main.tsx` 최상단(`App` 렌더링 전)에서 한 번 호출한다. GA4(`initializeAnalytics`)는 `router/index.tsx`에서 첫 `page_view`를 보내기 직전에 호출해야 하는 특수한 순서 제약이 있지만, Clarity는 페이지 이동마다 다시 호출할 API가 없어(스크립트 1회 삽입이 전부) 이런 제약이 없다. 두 모듈은 서로 다른 파일에서 독립적으로 초기화되어 충돌하지 않는다.

### SPA 페이지 이동

Clarity는 브라우저의 실제 클릭/스크롤/DOM 변화를 관찰하는 세션 리플레이 도구라, GA4의 `page_view`처럼 라우트 변경마다 별도로 호출해야 하는 API가 없다. 스크립트를 앱 시작 시 1회만 삽입하면 React Router의 클라이언트 사이드 이동도 같은 세션 안에서 자동으로 기록되므로, 라우터 쪽에는 별도 연동 코드가 없다.

### 개인정보 마스킹 정책

민감한 사용자 콘텐츠를 렌더링하는 최상위 컨테이너에 `data-clarity-mask="true"`를 적용해, Clarity 세션 리플레이에서 해당 영역의 텍스트/이미지가 가려지도록 했다. 버튼명·메뉴명·랜딩페이지 문구처럼 분석에 필요한 일반 UI 텍스트는 마스킹하지 않았다.

| 화면 | 마스킹 대상 |
|---|---|
| 로그인 / 회원가입 / 비밀번호 찾기 | 이메일·비밀번호·인증번호 입력 폼(`<form>`) 전체 |
| 작품 생성/상세/목록 | 제목·장르·설명이 담긴 카드(`.novel-info-card`, `.novel-card`) 및 생성 폼 |
| 회차 작성/수정/상세 | 번호·제목·본문 입력 폼, 회차 제목(`<h2>`), 본문(`.episode-content`) |
| AI 회차 요약 | 요약 텍스트(`.summary-text`) |
| 설정 충돌 감지 결과 | 충돌 카드(`ConflictCard` 루트, 기존 설정/현재 내용/AI 설명/제안 포함) |
| AI 등장인물·세계관 추출 결과 (회차 상세) | 결과 목록(`.episode-character-list`) |
| 등장인물 관리 / 참고 패널 | 인물 카드(`.item-card`, 이름·역할·나이·성격·말투·설명) |
| 세계관 관리 / 참고 패널 | 설정 카드(`.item-card`, 제목·내용) — 카테고리 목록 카드는 개수만 표시하므로 마스킹 제외 |
| 등장인물·세계관 AI 추출 검토 | 후보 카드(`.candidate-card`), 회차 제목(`.review-episode-title`) |
| AI 채팅 | 대화 영역(`.chat-messages`), 입력 영역(`.chat-input-area`) |

Access Token / Refresh Token / providerId / 사용자 ID는 애초에 화면에 렌더링되지 않으므로(로그인 로직 내부에서만 사용) 별도 마스킹 대상이 아니다.

### 환경변수

| 변수 | 설명 |
|---|---|
| `VITE_CLARITY_PROJECT_ID` | Microsoft Clarity 프로젝트 ID. 없으면 Clarity 비활성화(조용히 스킵) |

GA4와 달리 `.env.development`/`.env.production`에 값을 커밋하지 않는다 — Vercel Environment Variables(Production)에만 등록해서 로컬 개발 환경에서는 항상 비활성화되도록 한다.

### 운영 배포 후 확인 방법

1. Vercel 대시보드 → 해당 프로젝트 → Settings → Environment Variables → `VITE_CLARITY_PROJECT_ID` 등록 (Production 환경) → 재배포. Vite는 빌드 시점에 `import.meta.env.VITE_*` 값을 번들에 굳히므로, 등록 후 반드시 재배포해야 반영된다.
2. `https://www.novelnestia.com` 접속 → 브라우저 개발자 도구 → Network 탭에서 `clarity.ms/tag/<프로젝트ID>` 요청이 정확히 1번만 발생하는지 확인 (라우트를 여러 번 이동해도 추가 요청이 없어야 한다).
3. [Clarity 대시보드](https://clarity.microsoft.com) → 해당 프로젝트 → Recordings에서 몇 분 내로 새 세션이 잡히는지 확인하고, 로그인/회차 작성 등 마스킹 대상 화면을 리플레이로 열어 텍스트가 가려져 보이는지 육안으로 확인한다.

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
