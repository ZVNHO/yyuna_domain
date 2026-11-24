package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    // 새로고침하거나 주소를 직접 입력해도 Nuxt 화면이 열리게 연결
    @GetMapping({"/login", "/signup", "/yuna", "/find-id", "/reset-password"})
    public String forward() {
        return "forward:/index.html";
    }
}