package com.example.demo.controller.dto;

import com.example.demo.repository.entity.GuestbookReply;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GuestbookReplyResponse {
    private long id;
    private long guestbookId;
    private Long parentReplyId;  // 답글에 단 답글이면 원래 답글 id
    private String parentWriter; // "@이름" 표시용
    private String writer;
    private String content;
    private LocalDateTime createdAt;
    private boolean edited; // 수정한 적 있는지 ("수정됨" 표시용)
    private boolean mine; // 로그인한 회원이 쓴 답글인지 (수정·삭제 버튼 표시용)

    public static GuestbookReplyResponse responseReply(GuestbookReply entity, long loginMemberId) {
        GuestbookReplyResponse response = new GuestbookReplyResponse();
        response.id = entity.getId();
        response.guestbookId = entity.getGuestbookId();
        response.parentReplyId = entity.getParentReplyId();
        response.parentWriter = entity.getParentWriter();
        response.edited = entity.getUpdatedAt() != null;
        response.writer = entity.getWriter();
        response.content = entity.getContent();
        response.createdAt = entity.getCreatedAt();
        response.mine = entity.getMemberId() == loginMemberId;
        return response;
    }
}
