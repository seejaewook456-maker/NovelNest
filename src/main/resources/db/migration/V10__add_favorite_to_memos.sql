-- 메모 즐겨찾기 기능을 위한 memos.is_favorite 컬럼 추가
--
-- Character/WorldSetting의 isFavorite 컬럼과 동일한 규칙(TINYINT(1), 기본값 false)을 따른다.
-- 기존에 저장된 메모는 전부 즐겨찾기 해제 상태로 취급되어야 하므로 NOT NULL DEFAULT 0으로 두어
-- 컬럼 추가 시점에 이미 존재하는 행에도 안전하게 기본값이 채워지도록 한다.
--
-- V1~V9와 동일하게, 운영 DB에 이미 반영되어 있을 수도 있는 상태를 고려해 information_schema로
-- 컬럼 존재 여부를 먼저 확인한 뒤 없을 때만 DDL을 동적으로 실행한다(MySQL 버전 무관하게 안전,
-- 재실행해도 안전 — ADD COLUMN IF NOT EXISTS는 MySQL 8.0.29 미만에서 지원되지 않아 사용하지 않음).

SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'memos' AND COLUMN_NAME = 'is_favorite') = 0,
        'ALTER TABLE memos ADD COLUMN is_favorite TINYINT(1) NOT NULL DEFAULT 0 AFTER content',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
