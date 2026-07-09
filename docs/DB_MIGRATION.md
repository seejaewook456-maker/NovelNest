# DB 마이그레이션 (Flyway) 가이드

## 왜 도입했나

운영 환경은 `spring.jpa.hibernate.ddl-auto=validate`를 사용한다. 즉 Hibernate가 스키마를
자동으로 바꿔주지 않고, Entity와 실제 DB 컬럼이 다르면 아래처럼 애플리케이션 기동 자체가 실패한다.

```text
Schema-validation: missing column [refresh_token] in table [users]
```

이 문제를 막기 위해 운영 DB 스키마 변경은 **Flyway 마이그레이션 SQL**로만 관리한다.

## 동작 방식

```text
애플리케이션 시작
→ Flyway가 src/main/resources/db/migration의 SQL을 순서대로 적용
→ DB 스키마 최신화
→ Hibernate ddl-auto=validate 검증
→ Spring Boot 정상 기동
```

- 마이그레이션 파일 위치: `src/main/resources/db/migration/V{번호}__{설명}.sql`
- 운영(`prod`): `flyway.enabled=true`, `ddl-auto=validate` — Flyway로만 스키마 변경
- 로컬(`local`): `flyway.enabled=false`, `ddl-auto=update` — 기존처럼 Hibernate가 자동 반영 (개발 편의성 유지)

### 로컬에서 Flyway를 끈 이유

Flyway는 Hibernate보다 먼저 실행된다. 완전히 새로 만든 빈 로컬 DB에는 `users` 테이블 자체가
없는 상태이므로, `ALTER TABLE users ...` 형태의 마이그레이션이 실패한다. 로컬은 지금처럼
Hibernate(`update`)가 스키마를 만들도록 두는 대신, **운영에 반영해야 하는 스키마 변경은 반드시
Flyway SQL 파일로도 작성**해야 한다 (아래 체크리스트 참고).

### baseline-on-migrate

운영 DB는 이미 테이블이 있는 상태에서 Flyway를 처음 도입하므로(`flyway_schema_history` 테이블이
없음), `baseline-on-migrate: true` + `baseline-version: 0`으로 설정했다. 첫 배포 시 Flyway가
자동으로 baseline(버전 0)을 잡고, 그 이후 `V1__add_email_verification.sql`,
`V2__add_refresh_token.sql`을 실제로 실행한다. 두 스크립트 모두 `ADD COLUMN IF NOT EXISTS`,
`CREATE TABLE IF NOT EXISTS`로 작성되어 있어, 운영에 이미 수동으로 반영된 컬럼/테이블이 있어도
중복 오류 없이 안전하게 통과한다.

## Entity 필드 추가/수정 시 배포 전 체크리스트

1. Entity 변경
2. 관련 DTO/API 수정
3. `src/main/resources/db/migration`에 새 버전(`V3`, `V4`, ...)의 Flyway 마이그레이션 SQL 추가
   - MySQL 8.0.29+ 문법인 `ADD COLUMN IF NOT EXISTS` 등을 사용해 중복 실행에도 안전하게 작성
4. 로컬에서 `./gradlew bootRun`으로 Hibernate가 정상 반영되는지 확인
5. 운영 `ddl-auto`는 `validate` 유지 (절대 `update`로 바꾸지 말 것)
6. 배포 후 서버 로그에서 Flyway 마이그레이션 적용 로그(`Migrating schema` / `Successfully applied`) 확인
