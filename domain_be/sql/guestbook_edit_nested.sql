-- 방명록 수정 + 답글의 답글을 위한 DB 변경. 운영 DB에서 새 버전 배포 전에 한 번 실행하세요.
ALTER TABLE guestbook ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

ALTER TABLE guestbook_reply ADD COLUMN IF NOT EXISTS parent_reply_id BIGINT NULL AFTER guestbook_id;
ALTER TABLE guestbook_reply ADD COLUMN IF NOT EXISTS updated_at DATETIME NULL;

-- 원래 답글을 지우면 그 아래 답글도 함께 지운다
ALTER TABLE guestbook_reply
    ADD CONSTRAINT fk_guestbook_reply_parent FOREIGN KEY IF NOT EXISTS (parent_reply_id)
        REFERENCES guestbook_reply (id) ON DELETE CASCADE;
