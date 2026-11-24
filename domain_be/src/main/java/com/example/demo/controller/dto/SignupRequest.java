package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class SignupRequest {
    private String username;
    private String password;
    private String name;
    private String email;
}
