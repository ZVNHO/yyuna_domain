package com.example.demo.controller;

import com.example.demo.controller.dto.AccountResponse;
import com.example.demo.controller.dto.AccountUpdateRequest;
import com.example.demo.controller.dto.MessageResponse;
import com.example.demo.service.AccountService;
import com.example.demo.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

//마이페이지 (로그인한 회원만 사용)
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    private final AuthService authService;
    private final AccountService accountService;

    @GetMapping()// 내 정보
    public AccountResponse get(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return accountService.getAccount(authService.getLoginMember(authorization));
    }

    @PutMapping("/name")// 이름 변경
    public AccountResponse changeName(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody AccountUpdateRequest request) {
        return accountService.changeName(authService.getLoginMember(authorization), request);
    }

    @PutMapping("/password")// 비밀번호 변경 (현재 비밀번호 확인)
    public MessageResponse changePassword(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody AccountUpdateRequest request) {
        accountService.changePassword(authService.getLoginMember(authorization), request);
        return MessageResponse.createResponse("비밀번호를 바꿨어요.");
    }

    @PostMapping("/email/request")// 이메일 변경 1단계: 새 이메일로 인증 코드 발송
    public MessageResponse requestEmailChange(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody AccountUpdateRequest request) {
        accountService.requestEmailChange(authService.getLoginMember(authorization), request);
        return MessageResponse.createResponse("새 이메일로 인증 코드를 보냈어요.");
    }

    @PostMapping("/email/confirm")// 이메일 변경 2단계: 코드 확인 후 변경
    public AccountResponse confirmEmailChange(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody AccountUpdateRequest request) {
        return accountService.confirmEmailChange(authService.getLoginMember(authorization), request);
    }

    @PutMapping("/phone")// 휴대폰 변경 (새 번호 문자 인증 + 현재 비밀번호 확인)
    public AccountResponse changePhone(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody AccountUpdateRequest request) {
        return accountService.changePhone(authService.getLoginMember(authorization), request);
    }
}
