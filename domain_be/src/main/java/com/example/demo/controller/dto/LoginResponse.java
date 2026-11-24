package com.example.demo.controller.dto;

import com.example.demo.repository.entity.Member;
import lombok.Getter;

@Getter
public class LoginResponse {
    private String token;
    private String username;
    private String name;
    private boolean twoFactorRequired; // true면 token 없이 이메일 인증 코드를 기다리는 상태
    private String challenge;          // 2단계 요청(/auth/login/verify)에 함께 보낼 값
    private String maskedEmail;        // 코드를 보낸 주소 (일부 가림)

    public static LoginResponse responseLogin(Member entity) {
        LoginResponse response = new LoginResponse();
        response.token = entity.getToken();
        response.username = entity.getUsername();
        response.name = entity.getName();
        return response;
    }

    public static LoginResponse responseTwoFactor(String challenge, String maskedEmail) {
        LoginResponse response = new LoginResponse();
        response.twoFactorRequired = true;
        response.challenge = challenge;
        response.maskedEmail = maskedEmail;
        return response;
    }
}
