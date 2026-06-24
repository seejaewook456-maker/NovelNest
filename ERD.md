# ERD (Entity Relationship Diagram)

## 현재 구조

```
User
├── id (PK)
├── email (UNIQUE)
├── password
├── nickname
├── createdAt
└── updatedAt
    │
    │ 1:N
    ▼
Novel
├── id (PK)
├── user_id (FK → User.id)
├── title
├── genre
├── description
├── createdAt
└── updatedAt
    │
    ├── 1:N
    │   ▼
    │ Episode
    │ ├── id (PK)
    │ ├── novel_id (FK → Novel.id)
    │ ├── title
    │ ├── episodeNumber
    │ ├── content (TEXT)
    │ ├── createdAt
    │ └── updatedAt
    │     │
    │     │ 1:1
    │     ▼
    │   EpisodeSummary
    │   ├── id (PK)
    │   ├── episode_id (FK → Episode.id, UNIQUE)
    │   ├── summary (TEXT)
    │   ├── createdAt
    │   └── updatedAt
    │
    ├── 1:N
    │   ▼
    │ Character
    │ ├── id (PK)
    │ ├── novel_id (FK → Novel.id)
    │ ├── name
    │ ├── role (nullable)
    │ ├── age (nullable)
    │ ├── personality (TEXT, nullable)
    │ ├── speechStyle (TEXT, nullable)
    │ ├── description (TEXT, nullable)
    │ ├── createdAt
    │ └── updatedAt
    │     │
    │     │ N:M (EpisodeCharacter 중간 테이블)
    │     ▼
    │   EpisodeCharacter
    │   ├── id (PK)
    │   ├── episode_id (FK → Episode.id)
    │   ├── character_id (FK → Character.id)
    │   ├── createdAt
    │   └── updatedAt
    │   UNIQUE(episode_id, character_id)
    │
    └── 1:N
        ▼
      WorldSetting
      ├── id (PK)
      ├── novel_id (FK → Novel.id)
      ├── category (ENUM: COUNTRY/RACE/MAGIC/ORGANIZATION/PLACE/EVENT/ITEM/RULE/ETC)
      ├── title
      ├── content (TEXT)
      ├── createdAt
      └── updatedAt
```

---

## 테이블 관계 요약

| 관계 | 설명 |
|---|---|
| User : Novel = 1:N | 한 사용자는 여러 작품을 소유할 수 있다 |
| Novel : Episode = 1:N | 한 작품은 여러 회차를 가질 수 있다 |
| Novel : Character = 1:N | 한 작품은 여러 등장인물을 가질 수 있다 |
| Episode : Character = N:M | 한 회차에 여러 인물, 한 인물이 여러 회차에 등장 가능 (EpisodeCharacter 중간 테이블) |
| Novel : WorldSetting = 1:N | 한 작품은 여러 세계관 설정을 가질 수 있다 |

---

## 소유권 검증 체인

```
Episode.novel_id     → Novel.user_id → User.id
Character.novel_id   → Novel.user_id → User.id
WorldSetting.novel_id → Novel.user_id → User.id
```

모든 하위 리소스의 접근 권한은 Novel을 통해 User까지 거슬러 올라가서 검증한다.

---

## CharacterExtraction — DB 테이블 없음

CharacterExtraction은 DB를 사용하지 않는 순수 AI 분석 기능이다.
OpenAI가 반환한 후보(CharacterCandidateDto)는 메모리에서만 처리되며, 저장 시 기존 Character 테이블을 직접 사용한다.
저장 완료 시 EpisodeCharacter 테이블에 회차-인물 연결이 생성된다.

## Phase 완료 현황

| Phase | 내용 | 상태 |
|---|---|---|
| Phase 1 | Novel/Episode/Character/WorldSetting CRUD | 완료 |
| Phase 2-a | OpenAI 연동, EpisodeSummary | 완료 |
| Phase 2-b | CharacterExtraction (AI 후보 추출) | 완료 |
| Phase 2-c | EpisodeCharacter (회차별 인물 연결) | 완료 |
| Phase 2-d | 문체 분석, 설정 충돌 탐지 | 미구현 |
| Phase 3 | RAG 도입 | 미구현 |
