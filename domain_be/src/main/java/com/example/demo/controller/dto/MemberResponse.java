package com.example.demo.controller.dto;

import com.example.demo.repository.entity.Member;
import lombok.Getter;

@Getter
public class MemberResponse {
    private String username;
    private String name;
    private Long sessionSecondsLeft; // 로그인 만료까지 남은 초 (/auth/me, /auth/session/extend)

    public static MemberResponse responseMember(Member entity) {
        MemberResponse response = new MemberResponse();
        response.username = entity.getUsername();
        response.name = entity.getName();
        response.sessionSecondsLeft = entity.getSessionSecondsLeft();
        return response;
    }
}
