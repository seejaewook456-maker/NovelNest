-- AI 기능 하루 사용량 제한을 위한 사용자별·날짜별 카운터 테이블 생성
--
-- 사용자가 AI 기능을 호출할 때마다 오늘(Asia/Seoul 기준) 날짜의 행을 조회하고, 없으면 새로 생성한 뒤
-- 조건부 UPDATE(WHERE ... count < limit)로 원자적으로 증가시킨다. 매일 자정에 별도 스케줄러가 전체
-- 사용자 행을 일괄 초기화하는 방식은 쓰지 않는다 — (user_id, usage_date) 유니크 제약 덕분에 날짜가
-- 바뀌면 자연스럽게 새 행이 생성되어 "초기화"를 구현하며, 지난 날짜의 행은 이력으로 남는다.
--
-- V1~V5와 동일하게, 운영 DB에 이미 반영되어 있을 수도 있는 상태를 고려해 information_schema로
-- 존재 여부를 먼저 확인한 뒤 없을 때만 DDL을 동적으로 실행한다(MySQL 버전 무관하게 안전, 재실행해도 안전).

SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_daily_usages') = 0,
        'CREATE TABLE ai_daily_usages (
            id              BIGINT      NOT NULL AUTO_INCREMENT,
            user_id         BIGINT      NOT NULL,
            usage_date      DATE        NOT NULL,
            summary_count   INT         NOT NULL DEFAULT 0,
            conflict_count  INT         NOT NULL DEFAULT 0,
            character_count INT         NOT NULL DEFAULT 0,
            worldview_count INT         NOT NULL DEFAULT 0,
            chat_count      INT         NOT NULL DEFAULT 0,
            total_count     INT         NOT NULL DEFAULT 0,
            created_at      DATETIME(6) NOT NULL,
            updated_at      DATETIME(6) NOT NULL,
            PRIMARY KEY (id),
            UNIQUE KEY uk_ai_daily_usages_user_date (user_id, usage_date),
            CONSTRAINT fk_ai_daily_usages_user FOREIGN KEY (user_id) REFERENCES users (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
