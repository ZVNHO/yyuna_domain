-- 방명록 답글 / 자료실 기능을 위한 DB 변경. 운영 DB에서 한 번 실행하세요.

-- 1. 방명록 답글
CREATE TABLE IF NOT EXISTS guestbook_reply
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    guestbook_id BIGINT       NOT NULL,
    member_id    BIGINT       NOT NULL,
    content      VARCHAR(500) NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (guestbook_id) REFERENCES guestbook (id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

-- 2. 자료실 (파일 내용을 DB에 저장하므로 재배포해도 파일이 지워지지 않음)
CREATE TABLE IF NOT EXISTS library_file
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    file_size  BIGINT       NOT NULL,
    content    MEDIUMBLOB   NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
