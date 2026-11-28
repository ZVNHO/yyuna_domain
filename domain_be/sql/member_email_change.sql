-- 마이페이지 이메일 변경 인증 코드. 운영 DB에서 새 버전 배포 전에 한 번 실행하세요.
CREATE TABLE IF NOT EXISTS email_change
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL,          -- 바꾸려는 새 이메일 (코드는 이 주소로 보낸다)
    code_hash  VARCHAR(100) NOT NULL,
    attempts   INT          NOT NULL DEFAULT 0,
    expires_at DATETIME     NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
