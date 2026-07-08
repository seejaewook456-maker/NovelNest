-- 이메일 인증번호 기능 추가 마이그레이션
--
-- 이 프로젝트는 Flyway/Liquibase를 사용하지 않으며, 운영(prod) 프로필은
-- spring.jpa.hibernate.ddl-auto=validate 로 설정되어 있어 스키마를 자동으로 바꾸지 않는다.
-- 따라서 새 애플리케이션 이미지를 배포하기 전에 이 스크립트를 운영 DB에 수동으로 먼저 실행해야 한다.
-- (로컬은 ddl-auto=update 이므로 별도 작업 없이 Hibernate가 자동으로 반영한다.)

-- 1) users 테이블에 이메일 인증 여부 컬럼 추가
--    DEFAULT TRUE 로 추가하여, 이메일 인증 기능 도입 이전에 가입된 기존 계정이
--    컬럼 추가 직후에도 emailVerified=true 상태가 되어 로그인이 막히지 않도록 한다.
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- 2) 이메일 인증번호 저장 테이블 생성
CREATE TABLE email_verifications (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    email              VARCHAR(255) NOT NULL,
    verification_code  VARCHAR(6)   NOT NULL,
    verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at         DATETIME     NOT NULL,
    created_at         DATETIME     NOT NULL,
    verified_at        DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verifications_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
