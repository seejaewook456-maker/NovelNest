-- 비밀번호 재설정 기능 추가 마이그레이션
--
-- 1) email_verifications 테이블에 인증 목적(purpose) 컬럼을 추가해, 같은 이메일이라도
--    회원가입(SIGN_UP)과 비밀번호 재설정(PASSWORD_RESET) 인증을 서로 독립된 행으로 관리한다.
--    기존 회원가입 인증 데이터는 DEFAULT 'SIGN_UP'으로 채워져 컬럼 추가 직후에도 그대로 동작한다.
-- 2) 기존 email 단독 유니크 제약을 (email, purpose) 복합 유니크로 교체한다.
-- 3) 스케줄러의 만료 데이터 벌크 삭제(expires_at 기준)를 위한 인덱스를 추가한다.
-- 4) 비밀번호 재설정 전용 임시 토큰을 저장할 password_reset_tokens 테이블을 신규 생성한다.
--
-- V1/V2와 동일하게, 운영 DB에 이미 반영되어 있을 수도 있는 상태를 고려해 information_schema로
-- 존재 여부를 먼저 확인한 뒤 없을 때만 DDL을 동적으로 실행한다(MySQL 버전 무관하게 안전).

-- 1) email_verifications.purpose 컬럼 추가 (없을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_verifications' AND COLUMN_NAME = 'purpose') = 0,
        'ALTER TABLE email_verifications ADD COLUMN purpose VARCHAR(20) NOT NULL DEFAULT ''SIGN_UP''',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2-1) 기존 email 단독 유니크 키 제거 (있을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_verifications' AND INDEX_NAME = 'uk_email_verifications_email') > 0,
        'ALTER TABLE email_verifications DROP INDEX uk_email_verifications_email',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2-2) (email, purpose) 복합 유니크 키 추가 (없을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_verifications' AND INDEX_NAME = 'uk_email_verifications_email_purpose') = 0,
        'ALTER TABLE email_verifications ADD UNIQUE KEY uk_email_verifications_email_purpose (email, purpose)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) expires_at 인덱스 추가 (없을 때만) — 스케줄러의 만료 데이터 벌크 삭제 조건에 사용
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_verifications' AND INDEX_NAME = 'idx_email_verifications_expires_at') = 0,
        'ALTER TABLE email_verifications ADD INDEX idx_email_verifications_expires_at (expires_at)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) 비밀번호 재설정 임시 토큰 저장 테이블 생성 (없을 때만)
-- token_hash에는 원문 토큰이 아닌 SHA-256 해시(64자 hex)만 저장한다.
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'password_reset_tokens') = 0,
        'CREATE TABLE password_reset_tokens (
            id          BIGINT       NOT NULL AUTO_INCREMENT,
            email       VARCHAR(255) NOT NULL,
            token_hash  VARCHAR(64)  NOT NULL,
            expires_at  DATETIME     NOT NULL,
            used        BOOLEAN      NOT NULL DEFAULT FALSE,
            created_at  DATETIME     NOT NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_password_reset_tokens_token_hash (token_hash),
            KEY idx_password_reset_tokens_expires_at (expires_at),
            KEY idx_password_reset_tokens_email (email)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
