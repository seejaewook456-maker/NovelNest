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
