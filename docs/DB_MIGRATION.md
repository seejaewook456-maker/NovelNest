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
`V2__add_refresh_token.sql`을 실제로 실행한다.

기존에는 두 스크립트 모두 `ADD COLUMN IF NOT EXISTS` / `CREATE TABLE IF NOT EXISTS` 문법으로
작성되어 있었는데, **`ADD COLUMN IF NOT EXISTS`는 MySQL 8.0.29 이상에서만 지원되는 문법**이라
운영 MySQL 버전이 그보다 낮으면 문법 오류로 마이그레이션 자체가 실패하고
`flyway_schema_history`에 `success=0` 레코드가 남는 문제가 있었다. (`Detected failed migration to
version 1` 오류가 여기서 발생한다.)

그래서 두 스크립트는 MySQL 버전에 의존하지 않도록, `information_schema`로 컬럼/테이블 존재
여부를 먼저 조회한 뒤 없을 때만 `PREPARE`/`EXECUTE`로 DDL을 동적 실행하는 방식으로 작성돼 있다.
이 방식은 MySQL 5.x/8.x 어떤 버전에서도 동일하게 동작하며, 운영에 이미 수동으로 반영된
컬럼/테이블이 있으면 그대로 건너뛴다.

### 운영 DB 상태별 동작

- **기존 운영 DB** (컬럼/테이블 수동 반영 + `flyway_schema_history` 없음): 첫 기동 시 현재 상태를
  버전 0으로 baseline. 이어서 V1, V2가 실행되지만 `email_verified`, `email_verifications`,
  `refresh_token`이 이미 존재하므로 `information_schema` 조회 결과 아무 DDL도 실행하지 않고
  통과한다.
- **새로 세팅한 운영 DB** (완전히 빈 스키마): baseline 0을 잡은 뒤 V1, V2가 실제로 컬럼 추가/테이블
  생성을 수행해 스키마를 처음부터 구성한다.
- **로컬 DB**: `application-local.yml`에서 `flyway.enabled=false`이므로 Flyway는 아예 관여하지
  않고, 기존처럼 Hibernate(`ddl-auto=update`)가 스키마를 자동 반영한다.

## Entity 필드 추가/수정 시 배포 전 체크리스트

1. Entity 변경
2. 관련 DTO/API 수정
3. `src/main/resources/db/migration`에 새 버전(`V3`, `V4`, ...)의 Flyway 마이그레이션 SQL 추가
   - MySQL 버전에 의존하는 `ADD COLUMN IF NOT EXISTS` 대신, `information_schema` 조회 +
     `PREPARE`/`EXECUTE` 동적 DDL 패턴을 사용해 중복 실행 및 버전 차이에도 안전하게 작성
     (V1, V2 참고)
4. 로컬에서 `./gradlew bootRun`으로 Hibernate가 정상 반영되는지 확인
5. 운영 `ddl-auto`는 `validate` 유지 (절대 `update`로 바꾸지 말 것)
6. 배포 후 서버 로그에서 Flyway 마이그레이션 적용 로그(`Migrating schema` / `Successfully applied`) 확인

## 실패한 마이그레이션 복구 절차

`flyway_schema_history`에 `success=0`인 레코드가 남아있으면, Flyway는 다음 기동 시
`validate` 단계에서 곧바로 실패하고 애플리케이션이 뜨지 않는다. 아래 순서로 복구한다.

1. **원인이 된 마이그레이션 SQL을 먼저 수정한다.** (이미 V1/V2는 `information_schema` 기반으로
   수정됨) 실패했던 스크립트를 그대로 둔 채 기록만 지우면 재기동 시 같은 이유로 다시 실패한다.
2. **실패 기록을 정리한다.** 아래 두 방법 중 하나를 사용한다.
   - `flyway repair` 커맨드(Flyway CLI 또는 Gradle/Maven 플러그인 사용 시): 실패한 행을
     `flyway_schema_history`에서 제거해준다.
   - 위 도구가 없다면 운영 DB에 직접 접속해 실패한 행만 삭제한다.
     ```sql
     DELETE FROM flyway_schema_history WHERE version = '1' AND success = 0;
     ```
     성공한 행(`success = 1`)은 절대 건드리지 않는다.
3. **애플리케이션을 재기동한다.** Flyway가 V1부터 다시 실행되며, 수정된 SQL은 이미 존재하는
   컬럼/테이블을 건드리지 않고 통과한 뒤 `flyway_schema_history`에 정상적으로 `success = 1`로
   기록된다.
4. 재기동 로그에서 `Successfully applied 1 migration` 등의 로그로 정상 적용을 확인한다.
