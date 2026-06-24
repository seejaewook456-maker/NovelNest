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

## Phase 1 완료 — MVP 콘텐츠 구조

Novel을 중심으로 Episode, Character, WorldSetting 모두 구현 완료.
