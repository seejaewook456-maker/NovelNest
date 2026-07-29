-- AI 하루 사용량 전체 합계 제한 폐지 — ai_daily_usages.total_count 컬럼 제거
--
-- 전체 100회 합계 제한은 두지 않고 기능별 제한(회차 요약/설정 충돌 감지/등장인물 추출/세계관 추출/
-- AI 챗봇)만 유지하기로 정책을 변경했다. total_count는 더 이상 어떤 코드에서도 읽거나 쓰지 않으므로
-- 컬럼 자체를 제거한다.
--
-- V1~V6과 동일하게, 운영 DB 상태를 고려해 information_schema로 존재 여부를 먼저 확인한 뒤 있을
-- 때만 DDL을 동적으로 실행한다(재실행해도 안전).

SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_daily_usages' AND COLUMN_NAME = 'total_count') > 0,
        'ALTER TABLE ai_daily_usages DROP COLUMN total_count',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
