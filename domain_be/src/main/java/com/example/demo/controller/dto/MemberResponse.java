package com.example.demo.controller.dto;

import com.example.demo.repository.entity.Member;
import lombok.Getter;

@Getter
public class MemberResponse {
    private String username;
    private String name;

    public static MemberResponse responseMember(Member entity) {
        MemberResponse response = new MemberResponse();
        response.username = entity.getUsername();
        response.name = entity.getName();
        return response;
    }
}
