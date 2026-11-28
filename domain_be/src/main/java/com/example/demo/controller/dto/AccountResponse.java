package com.example.demo.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 마이페이지에 보여줄 내 정보 (비밀번호·토큰은 내려주지 않는다)
@Getter
@AllArgsConstructor
public class AccountResponse {
    private String username;
    private String name;
    private String email;  // 없으면 null (예전 회원)
    private String phone;  // 010-1234-5678 형식, 없으면 null
    private boolean phoneVerification; // 서버에 문자 설정이 있어 휴대폰을 바꿀 수 있는지
}
