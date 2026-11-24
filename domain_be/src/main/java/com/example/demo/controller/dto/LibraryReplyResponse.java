package com.example.demo.controller.dto;

import com.example.demo.repository.entity.LibraryReply;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class LibraryReplyResponse {
    private long id;
    private long fileId;
    private String writer;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine; // 로그인한 회원이 쓴 답글인지 (삭제 버튼 표시용)

    public static LibraryReplyResponse responseReply(LibraryReply entity, long loginMemberId) {
        LibraryReplyResponse response = new LibraryReplyResponse();
        response.id = entity.getId();
        response.fileId = entity.getFileId();
        response.writer = entity.getWriter();
        response.content = entity.getContent();
        response.createdAt = entity.getCreatedAt();
        response.mine = entity.getMemberId() == loginMemberId;
        return response;
    }
}
