CREATE TABLE IF NOT EXISTS yuna
(
    yuna  INT AUTO_INCREMENT PRIMARY KEY,
    age   INT,
    hobby VARCHAR(100),
    job   VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS member
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NULL UNIQUE,
    token      VARCHAR(100) NULL UNIQUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

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

CREATE TABLE IF NOT EXISTS guestbook
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NULL,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
    );

CREATE TABLE IF NOT EXISTS guestbook_reply
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    guestbook_id    BIGINT       NOT NULL,
    parent_reply_id BIGINT       NULL,
    member_id       BIGINT       NOT NULL,
    content         VARCHAR(500) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NULL,
    FOREIGN KEY (guestbook_id) REFERENCES guestbook (id) ON DELETE CASCADE,
    FOREIGN KEY (parent_reply_id) REFERENCES guestbook_reply (id) ON DELETE CASCADE,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
    );

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

INSERT INTO yuna (yuna, age, hobby, job)
SELECT 1, 24, '베이킹', '개발자'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM yuna WHERE yuna = 1);