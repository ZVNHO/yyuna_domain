package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class MessageResponse {
    private String message;

    public static MessageResponse createResponse(String message) {
        MessageResponse response = new MessageResponse();
        response.message = message;
        return response;
    }
}
