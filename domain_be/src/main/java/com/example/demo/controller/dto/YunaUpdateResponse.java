package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class YunaUpdateResponse {

    public String message;

    public static YunaUpdateResponse createResponse(String message) {
        YunaUpdateResponse response = new YunaUpdateResponse();
        response.message = message;
        return response;
    }
}
