package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class SignupRequest {
    private String username;
    private String password;
    private String name;
    private String email;
    private String phone;                  // 문자 인증이 켜져 있을 때만 필수
    private String phoneVerificationToken; // /auth/phone/verify에서 받은 값
}
