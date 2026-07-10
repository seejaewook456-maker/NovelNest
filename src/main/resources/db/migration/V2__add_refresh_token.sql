-- Refresh Token 기반 Access Token 재발급 기능 추가 마이그레이션
--
-- 운영 DB에는 Flyway 도입 이전에 이미 이 컬럼이 수동으로 반영되어 있을 수 있다.
-- "ALTER TABLE ... ADD COLUMN IF NOT EXISTS" 는 MySQL 8.0.29 이상에서만 지원되는 문법이라,
-- 운영 MySQL 버전이 더 낮으면 문법 오류로 마이그레이션 자체가 실패할 수 있다.
-- 따라서 information_schema로 컬럼 존재 여부를 먼저 확인한 뒤, 없을 때만 ALTER TABLE을
-- 동적으로 실행하는 방식으로 작성해 MySQL 버전에 관계없이 안전하게 동작하도록 한다.

-- users 테이블에 Refresh Token 저장 컬럼 추가 (없을 때만)
-- NULL 허용: 기존 가입자는 재로그인 전까지 Refresh Token이 없는 상태이며,
-- 로그아웃 시에도 NULL로 되돌려 재발급을 무효화한다.
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'refresh_token') = 0,
        'ALTER TABLE users ADD COLUMN refresh_token VARCHAR(512) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
