package com.example.demo.controller.dto;

import lombok.Getter;

@Getter
public class AvailabilityResponse {
    private boolean available;

    public static AvailabilityResponse createResponse(boolean available) {
        AvailabilityResponse response = new AvailabilityResponse();
        response.available = available;
        return response;
    }
}
