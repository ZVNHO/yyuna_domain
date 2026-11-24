package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Guestbook {
    private long id;
    private long memberId;
    private String writer;   // member.name (조회 시 JOIN)
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 수정한 적 없으면 null

    public static Guestbook createGuestbook(long memberId, String content) {
        Guestbook guestbook = new Guestbook();
        guestbook.memberId = memberId;
        guestbook.content = content;
        return guestbook;
    }
}
