-- Refresh Token 기반 Access Token 재발급 기능 추가 마이그레이션
--
-- 이 프로젝트는 Flyway/Liquibase를 사용하지 않으며, 운영(prod) 프로필은
-- spring.jpa.hibernate.ddl-auto=validate 로 설정되어 있어 스키마를 자동으로 바꾸지 않는다.
-- 따라서 새 애플리케이션 이미지를 배포하기 전에 이 스크립트를 운영 DB에 수동으로 먼저 실행해야 한다.
-- (로컬은 ddl-auto=update 이므로 별도 작업 없이 Hibernate가 자동으로 반영한다.)

-- users 테이블에 Refresh Token 저장 컬럼 추가
-- NULL 허용: 기존 가입자는 재로그인 전까지 Refresh Token이 없는 상태이며,
-- 로그아웃 시에도 NULL로 되돌려 재발급을 무효화한다.
ALTER TABLE users
    ADD COLUMN refresh_token VARCHAR(512) NULL;
