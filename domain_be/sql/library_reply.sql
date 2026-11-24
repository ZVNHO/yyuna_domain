-- 자료실 답글 기능을 위한 DB 변경. 운영 DB에서 한 번 실행하세요.
CREATE TABLE IF NOT EXISTS library_reply
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_id    BIGINT       NOT NULL,
    member_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_id) REFERENCES library_file (id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);
