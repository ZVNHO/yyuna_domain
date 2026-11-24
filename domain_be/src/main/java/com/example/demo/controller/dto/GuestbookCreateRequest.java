package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class GuestbookCreateRequest {
    private String content;
    private Long parentReplyId; // 방명록 답글에 다시 답글을 달 때만 사용 (자료실 답글은 무시)
}
