package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GuestbookReply {
    private long id;
    private long guestbookId;
    private Long parentReplyId; // 답글에 단 답글이면 원래 답글 id, 방명록 글에 바로 단 답글이면 null
    private long memberId;
    private String writer;      // member.name (조회 시 JOIN)
    private String parentWriter; // 원래 답글 작성자 이름 (조회 시 JOIN, "@이름" 표시용)
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 수정한 적 없으면 null

    public static GuestbookReply createReply(long guestbookId, Long parentReplyId, long memberId, String content) {
        GuestbookReply reply = new GuestbookReply();
        reply.guestbookId = guestbookId;
        reply.parentReplyId = parentReplyId;
        reply.memberId = memberId;
        reply.content = content;
        return reply;
    }
}
