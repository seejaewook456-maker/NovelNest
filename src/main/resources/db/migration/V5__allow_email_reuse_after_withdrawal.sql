-- 회원 탈퇴 후 14일 재가입 허용 정책 마이그레이션
--
-- 기존에는 users.email에 단순 UNIQUE 제약이 걸려 있어, 탈퇴한 회원의 이메일을 DB에 남겨둔 채로는
-- 같은 이메일로 새 User 행을 만들 수 없었다(탈퇴 회원을 복구하는 방식이 아니라 신규 생성해야 하므로
-- 같은 이메일을 가진 행이 "탈퇴 이력 + 신규 활성 회원" 두 개 동시에 존재해야 한다).
--
-- 이를 위해 email 단독 UNIQUE 제약을 제거하고, 대신 "활성 회원(deleted_at IS NULL)일 때만 실제 값을
-- 갖고 탈퇴 회원은 NULL이 되는" 생성 컬럼에 UNIQUE 인덱스를 건다. MySQL은 UNIQUE 인덱스에서 NULL을
-- 여러 번 허용하므로, 탈퇴 회원들의 NULL 값끼리는 충돌하지 않고 "활성 회원끼리만 이메일 유일"이라는
-- 제약을 DB 레벨에서 계속 보장할 수 있다(PostgreSQL의 partial unique index를 MySQL에서 흉내낸 것).
-- provider+providerId(OAuth 계정 식별자)에도 같은 패턴을 적용한다.
--
-- V1~V4와 동일하게, 운영 DB 상태를 고려해 information_schema로 존재 여부를 먼저 확인한 뒤
-- 없을 때만 DDL을 동적으로 실행한다(MySQL 버전 무관하게 안전, 재실행해도 안전).

-- 1) users.email에 걸린 기존 UNIQUE 인덱스 제거 (Hibernate ddl-auto=update가 자동 생성한 이름이라
--    정확한 이름을 알 수 없으므로, information_schema에서 email 컬럼의 유니크 인덱스 이름을 조회해 동적으로 제거)
SET @email_unique_index := (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'email' AND NON_UNIQUE = 0 AND INDEX_NAME != 'PRIMARY'
    LIMIT 1
);
SET @stmt = (
    SELECT IF(
        @email_unique_index IS NOT NULL,
        CONCAT('ALTER TABLE users DROP INDEX ', @email_unique_index),
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 이메일 조회 성능 유지를 위한 일반(비유니크) 인덱스 추가 (없을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'idx_users_email') = 0,
        'ALTER TABLE users ADD INDEX idx_users_email (email)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) 활성 회원일 때만 값을 갖는 생성 컬럼 추가 (없을 때만) — 탈퇴 회원(deleted_at NOT NULL)은 NULL
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_active_key') = 0,
        'ALTER TABLE users ADD COLUMN email_active_key VARCHAR(255)
            GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN email ELSE NULL END) STORED',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4) email_active_key에 UNIQUE 인덱스 추가 (없을 때만) — 활성 회원끼리만 이메일 유일성 보장
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uq_users_email_active_key') = 0,
        'ALTER TABLE users ADD UNIQUE INDEX uq_users_email_active_key (email_active_key)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5) provider+providerId도 동일한 패턴 적용 — 활성 회원일 때만 값을 갖는 생성 컬럼 추가 (없을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'provider_identity_active_key') = 0,
        'ALTER TABLE users ADD COLUMN provider_identity_active_key VARCHAR(512)
            GENERATED ALWAYS AS (
                CASE WHEN deleted_at IS NULL AND provider_id IS NOT NULL
                     THEN CONCAT(provider, '':'', provider_id)
                     ELSE NULL END
            ) STORED',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 6) provider_identity_active_key에 UNIQUE 인덱스 추가 (없을 때만) — 활성 회원끼리만 OAuth 계정 유일성 보장
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND INDEX_NAME = 'uq_users_provider_identity_active_key') = 0,
        'ALTER TABLE users ADD UNIQUE INDEX uq_users_provider_identity_active_key (provider_identity_active_key)',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
