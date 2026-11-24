package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class PasswordResetRequest {
    private String username;
    private String email;
}
