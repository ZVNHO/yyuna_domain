-- 로그인 2단계(이메일) 인증을 위한 DB 변경. 운영 DB에서 새 버전 배포 전에 한 번 실행하세요.
CREATE TABLE IF NOT EXISTS login_code
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL UNIQUE,
    challenge  VARCHAR(100) NOT NULL UNIQUE,
    code_hash  VARCHAR(100) NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
