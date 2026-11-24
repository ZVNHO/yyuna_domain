package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class LoginVerifyRequest {
    private String challenge;
    private String code;
}
