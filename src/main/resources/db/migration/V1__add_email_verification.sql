-- 이메일 인증번호 기능 추가 마이그레이션
--
-- 운영 DB에는 Flyway 도입 이전에 이미 이 내용이 수동으로 반영되어 있을 수 있다.
-- "ALTER TABLE ... ADD COLUMN IF NOT EXISTS" 는 MySQL 8.0.29 이상에서만 지원되는 문법이라,
-- 운영 MySQL 버전이 더 낮으면 문법 오류로 마이그레이션 자체가 실패할 수 있다.
-- 따라서 information_schema로 컬럼/테이블 존재 여부를 먼저 확인한 뒤, 없을 때만 DDL을
-- 동적으로 실행하는 방식으로 작성해 MySQL 버전에 관계없이 안전하게 동작하도록 한다.

-- 1) users 테이블에 이메일 인증 여부 컬럼 추가 (없을 때만)
--    DEFAULT TRUE 로 추가하여, 이메일 인증 기능 도입 이전에 가입된 기존 계정이
--    컬럼 추가 직후에도 emailVerified=true 상태가 되어 로그인이 막히지 않도록 한다.
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified') = 0,
        'ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 이메일 인증번호 저장 테이블 생성 (없을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_verifications') = 0,
        'CREATE TABLE email_verifications (
            id                 BIGINT       NOT NULL AUTO_INCREMENT,
            email              VARCHAR(255) NOT NULL,
            verification_code  VARCHAR(6)   NOT NULL,
            verified           BOOLEAN      NOT NULL DEFAULT FALSE,
            expires_at         DATETIME     NOT NULL,
            created_at         DATETIME     NOT NULL,
            verified_at        DATETIME     NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_email_verifications_email (email)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
