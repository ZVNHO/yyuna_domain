package com.example.demo.controller;

import com.example.demo.controller.dto.NewsResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

//개발 뉴스 (로그인한 회원만 사용)
@RestController
@RequiredArgsConstructor
public class NewsController {
    private final AuthService authService;
    private final NewsService newsService;

    @GetMapping("/news")
    public NewsResponse latest(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.getLoginMember(authorization);
        return newsService.getLatest();
    }
}
