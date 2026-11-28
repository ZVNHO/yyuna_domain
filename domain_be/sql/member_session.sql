-- 로그인 세션 30분 만료를 위한 DB 변경. 운영 DB에서 새 버전 배포 전에 한 번 실행하세요.
-- (실행하면 기존 로그인은 모두 풀려서 다시 로그인해야 해요)
ALTER TABLE member ADD COLUMN IF NOT EXISTS token_expires_at DATETIME NULL AFTER token;
