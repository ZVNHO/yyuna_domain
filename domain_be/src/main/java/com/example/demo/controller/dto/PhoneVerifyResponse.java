package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class PhoneVerifyResponse {
    private String verificationToken; // 회원가입 요청에 phoneVerificationToken으로 보낸다

    public static PhoneVerifyResponse createResponse(String verificationToken) {
        PhoneVerifyResponse response = new PhoneVerifyResponse();
        response.verificationToken = verificationToken;
        return response;
    }
}
