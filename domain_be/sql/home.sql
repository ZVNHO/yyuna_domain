-- 홈페이지(로그인/회원가입/방명록) 기능을 위한 DB 변경. demo DB에서 한 번 실행하세요.
USE demo;

-- 1. 직업(job)을 숫자 → 문자열로 변경
ALTER TABLE yuna MODIFY job VARCHAR(50);

-- 2. 회원
CREATE TABLE IF NOT EXISTS member
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    token      VARCHAR(100) NULL UNIQUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. 방명록
CREATE TABLE IF NOT EXISTS guestbook
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    content    VARCHAR(500) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
);

-- 4. 내 소개 탭에 표시될 유나 정보 (yuna = 1)
UPDATE yuna
SET age = 24, hobby = '베이킹', job = '개발자'
WHERE yuna = 1;

INSERT INTO yuna (yuna, age, hobby, job)
SELECT 1, 24, '베이킹', '개발자'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM yuna WHERE yuna = 1);
