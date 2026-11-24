package com.example.demo.controller.dto;

import com.example.demo.repository.entity.Yuna;
import lombok.Getter;

@Getter
public class
YunaGetResponse {
    private int yuna;
    private int age;
    private  String hobby;
    private String job;


    public static YunaGetResponse responseYuna(Yuna entity) {
        YunaGetResponse response = new YunaGetResponse();
        response.yuna = entity.getYuna();
        response.age = entity.getAge();
        response.hobby = entity.getHobby();
        response.job = entity.getJob();
        return response;

    }
}
