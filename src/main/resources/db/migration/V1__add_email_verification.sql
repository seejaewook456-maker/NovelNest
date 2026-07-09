-- 이메일 인증번호 기능 추가 마이그레이션
--
-- 운영 DB에는 Flyway 도입 이전에 이미 이 내용이 수동으로 반영되어 있을 수 있으므로,
-- IF NOT EXISTS를 사용해 여러 번 실행되어도 안전하게(중복 오류 없이) 동작하도록 작성한다.

-- 1) users 테이블에 이메일 인증 여부 컬럼 추가
--    DEFAULT TRUE 로 추가하여, 이메일 인증 기능 도입 이전에 가입된 기존 계정이
--    컬럼 추가 직후에도 emailVerified=true 상태가 되어 로그인이 막히지 않도록 한다.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT TRUE;

-- 2) 이메일 인증번호 저장 테이블 생성
CREATE TABLE IF NOT EXISTS email_verifications (
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
