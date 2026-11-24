package com.example.demo.controller.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class YunaCreateResponse {
    int count;
    String message;

    public static YunaCreateResponse success(int count, String  message) {
        return new YunaCreateResponse(count, message);
    }

    public YunaCreateResponse(int count, String message) {
        this.count = count;
        this.message = message;
    }

}
