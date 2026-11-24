package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class PasswordResetConfirmRequest {
    private String username;
    private String code;
    private String newPassword;
}
