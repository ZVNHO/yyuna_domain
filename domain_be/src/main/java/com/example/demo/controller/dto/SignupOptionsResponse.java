package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class SignupOptionsResponse {
    private boolean phoneVerification; // 문자 인증이 설정돼 있으면 회원가입에서 휴대폰 인증을 받는다

    public static SignupOptionsResponse createResponse(boolean phoneVerification) {
        SignupOptionsResponse response = new SignupOptionsResponse();
        response.phoneVerification = phoneVerification;
        return response;
    }
}
