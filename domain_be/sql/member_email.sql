-- 아이디·비밀번호 찾기를 위한 DB 변경. 운영 DB에서 새 버전 배포 전에 한 번 실행하세요.
ALTER TABLE member ADD COLUMN IF NOT EXISTS email VARCHAR(100) NULL UNIQUE AFTER name;

-- 비밀번호 재설정 인증 코드 (회원당 1개, 코드는 BCrypt로 저장)
CREATE TABLE IF NOT EXISTS password_reset
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL UNIQUE,
    code_hash  VARCHAR(100) NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
