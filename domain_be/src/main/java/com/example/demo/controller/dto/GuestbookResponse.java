package com.example.demo.controller.dto;

import com.example.demo.repository.entity.Guestbook;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class GuestbookResponse {
    private long id;
    private String writer;
    private String content;
    private LocalDateTime createdAt;
    private boolean edited; // 수정한 적 있는지 ("수정됨" 표시용)
    private boolean mine; // 로그인한 회원이 쓴 글인지 (수정·삭제 버튼 표시용)
    private List<GuestbookReplyResponse> replies;

    public static GuestbookResponse responseGuestbook(Guestbook entity, long loginMemberId, List<GuestbookReplyResponse> replies) {
        GuestbookResponse response = new GuestbookResponse();
        response.id = entity.getId();
        response.writer = entity.getWriter();
        response.content = entity.getContent();
        response.createdAt = entity.getCreatedAt();
        response.edited = entity.getUpdatedAt() != null;
        response.mine = entity.getMemberId() == loginMemberId;
        response.replies = replies;
        return response;
    }
}
