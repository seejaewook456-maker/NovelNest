-- 분당 AI Rate Limit(최근 60초 Sliding Window) 판단을 위한 사용자별 요청 시각 로그 테이블 생성
--
-- 기능별이 아니라 "AI 전체 요청" 합계 기준으로 최근 60초 동안 몇 번 요청했는지 세기 위한 최소 컬럼만
-- 둔다(id, user_id, requested_at). Redis 없이 이 테이블만으로 처리하며, 오래된 로그는 별도 배치 없이
-- Rate Limit 체크 시점마다 윈도우 밖으로 나간 행을 함께 삭제해 테이블이 무한히 커지지 않게 한다
-- (AiRateLimitService 참고).
--
-- 인덱스 2개를 둔 이유:
--   idx_ai_request_logs_user_requested (user_id, requested_at) — 사용자별 최근 N초 요청 수 COUNT 쿼리용
--   idx_ai_request_logs_requested_at (requested_at)            — 사용자 무관 전체 오래된 로그 삭제(DELETE)용
--
-- V1~V7과 동일하게, 운영 DB에 이미 반영되어 있을 수도 있는 상태를 고려해 information_schema로
-- 존재 여부를 먼저 확인한 뒤 없을 때만 DDL을 동적으로 실행한다(재실행해도 안전).

SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_request_logs') = 0,
        'CREATE TABLE ai_request_logs (
            id           BIGINT      NOT NULL AUTO_INCREMENT,
            user_id      BIGINT      NOT NULL,
            requested_at DATETIME(6) NOT NULL,
            PRIMARY KEY (id),
            KEY idx_ai_request_logs_user_requested (user_id, requested_at),
            KEY idx_ai_request_logs_requested_at (requested_at),
            CONSTRAINT fk_ai_request_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
