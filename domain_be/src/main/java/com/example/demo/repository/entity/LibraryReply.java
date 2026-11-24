package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LibraryReply {
    private long id;
    private long fileId;
    private long memberId;
    private String writer;   // member.name (조회 시 JOIN)
    private String content;
    private LocalDateTime createdAt;

    public static LibraryReply createReply(long fileId, long memberId, String content) {
        LibraryReply reply = new LibraryReply();
        reply.fileId = fileId;
        reply.memberId = memberId;
        reply.content = content;
        return reply;
    }
}
