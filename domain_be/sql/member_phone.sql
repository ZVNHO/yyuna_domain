-- 문자(SMS) 2단계 인증을 위한 DB 변경. 운영 DB에서 새 버전 배포 전에 한 번 실행하세요.
ALTER TABLE member ADD COLUMN IF NOT EXISTS phone VARCHAR(20) NULL UNIQUE AFTER email;

-- 회원가입 때 휴대폰 번호 인증 (번호당 1개, 코드는 BCrypt로 저장)
CREATE TABLE IF NOT EXISTS phone_verification
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone             VARCHAR(20)  NOT NULL UNIQUE,
    code_hash         VARCHAR(100) NOT NULL,
    attempts          INT          NOT NULL DEFAULT 0,
    sent_count        INT          NOT NULL DEFAULT 1, -- 하루 발송 횟수 (문자 요금 남용 방지)
    window_started_at DATETIME     NOT NULL,
    last_sent_at      DATETIME     NOT NULL,
    expires_at        DATETIME     NOT NULL,
    verified_token    VARCHAR(100) NULL UNIQUE,        -- 인증을 마치면 발급, 회원가입 요청에 함께 보낸다
    verified_at       DATETIME     NULL
);
