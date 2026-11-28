package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class PhoneCodeRequest {
    private String phone;
    private String code; // 인증번호 확인 때만 사용
}
