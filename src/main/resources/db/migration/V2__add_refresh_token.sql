-- Refresh Token 기반 Access Token 재발급 기능 추가 마이그레이션
--
-- 운영 DB에는 Flyway 도입 이전에 이미 이 컬럼이 수동으로 반영되어 있으므로,
-- IF NOT EXISTS를 사용해 여러 번 실행되어도 안전하게(중복 오류 없이) 동작하도록 작성한다.

-- users 테이블에 Refresh Token 저장 컬럼 추가
-- NULL 허용: 기존 가입자는 재로그인 전까지 Refresh Token이 없는 상태이며,
-- 로그아웃 시에도 NULL로 되돌려 재발급을 무효화한다.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS refresh_token VARCHAR(512) NULL;
