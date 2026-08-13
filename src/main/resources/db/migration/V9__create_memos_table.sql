-- 작품별 개인 메모(Memo) 기능을 위한 테이블 생성
--
-- Episode/Character/WorldSetting과 동일하게 novel_id FK로 Novel에 직접 소속되며,
-- 회차 번호 같은 순번 개념 없이 title/content(TEXT)만 갖는 단순 구조다. Novel 삭제 시
-- Episode/Character/WorldSetting과 동일한 정책(NovelService에서 하위 리소스를 먼저 명시적으로
-- 삭제)을 따르므로 FK에 ON DELETE CASCADE는 두지 않는다(다른 하위 테이블들과 일관성 유지).
--
-- 목록 조회를 "최근 수정순"으로 정렬하므로 (novel_id, updated_at) 복합 인덱스를 둔다.
--
-- V1~V8과 동일하게, 운영 DB에 이미 반영되어 있을 수도 있는 상태를 고려해 information_schema로
-- 존재 여부를 먼저 확인한 뒤 없을 때만 DDL을 동적으로 실행한다(MySQL 버전 무관하게 안전, 재실행해도 안전).

SET @stmt = (
    SELECT IF(
        (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'memos') = 0,
        'CREATE TABLE memos (
            id         BIGINT      NOT NULL AUTO_INCREMENT,
            novel_id   BIGINT      NOT NULL,
            title      VARCHAR(255) NOT NULL,
            content    TEXT        NOT NULL,
            created_at DATETIME(6) NOT NULL,
            updated_at DATETIME(6) NOT NULL,
            PRIMARY KEY (id),
            KEY idx_memos_novel_updated (novel_id, updated_at),
            CONSTRAINT fk_memos_novel FOREIGN KEY (novel_id) REFERENCES novels (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4',
        'SELECT 1'
    )
);
PREPARE stmt FROM @stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
