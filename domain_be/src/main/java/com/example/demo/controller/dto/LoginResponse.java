package com.example.demo.controller.dto;

import com.example.demo.repository.entity.Member;
import lombok.Getter;

@Getter
public class LoginResponse {
    private String token;
    private String username;
    private String name;
    private boolean twoFactorRequired; // true면 token 없이 인증 코드를 기다리는 상태
    private String challenge;          // 2단계 요청(/auth/login/verify)에 함께 보낼 값
    private String channel;            // 코드를 보낸 방법: "sms" 또는 "email"
    private String sentTo;             // 코드를 보낸 곳 (일부 가림: 010-****-5678, yu****@gmail.com)
    private Long sessionSecondsLeft;   // 로그인 만료까지 남은 초 (30분)

    public static LoginResponse responseLogin(Member entity) {
        LoginResponse response = new LoginResponse();
        response.token = entity.getToken();
        response.username = entity.getUsername();
        response.name = entity.getName();
        response.sessionSecondsLeft = entity.getSessionSecondsLeft();
        return response;
    }

    public static LoginResponse responseTwoFactor(String challenge, String channel, String sentTo) {
        LoginResponse response = new LoginResponse();
        response.twoFactorRequired = true;
        response.challenge = challenge;
        response.channel = channel;
        response.sentTo = sentTo;
        return response;
    }
}
