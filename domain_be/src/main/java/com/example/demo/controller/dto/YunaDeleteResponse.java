package com.example.demo.controller.dto;

public class YunaDeleteResponse {

    public String message;

    public static YunaDeleteResponse createResponse(String message) {
        YunaDeleteResponse response1 = new YunaDeleteResponse();
        response1.message = message;
        return response1;
    }
}
