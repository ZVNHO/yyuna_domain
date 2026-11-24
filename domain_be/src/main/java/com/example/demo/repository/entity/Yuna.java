package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Yuna {
    private int yuna;
    private int age;
    private  String hobby;
    private String job;

    public static Yuna createYuna(int yuna, int age, String hobby, String job){
        Yuna me = new Yuna();
        me.age=age;
        me.hobby=hobby;
        me.job= job;
        me.yuna= yuna;
        return me;
    }



    public void internal() {

    }

}
