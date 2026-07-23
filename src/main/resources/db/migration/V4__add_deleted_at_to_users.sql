-- 회원 탈퇴(소프트 삭제) 기능 추가 마이그레이션
--
-- users 테이블에 deleted_at 컬럼을 추가한다.
-- - NULL: 정상 회원
-- - NOT NULL: 탈퇴한 회원 (탈퇴 시각 기록, 물리 삭제는 하지 않음)
--
-- V1~V3와 동일하게, 운영 DB에 이미 반영되어 있을 수도 있는 상태를 고려해 information_schema로
-- 존재 여부를 먼저 확인한 뒤 없을 때만 DDL을 동적으로 실행한다(MySQL 버전 무관하게 안전).

-- users.deleted_at 컬럼 추가 (없을 때만)
SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'deleted_at') = 0,
        'ALTER TABLE users ADD COLUMN deleted_at DATETIME(6) NULL',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
