package com.example.demo.controller.dto;

import lombok.Getter;
import lombok.Setter;

// 마이페이지 수정 요청. 항목마다 필요한 값만 채워 보낸다
@Getter
@Setter
public class AccountUpdateRequest {
    private String name;              // 이름 변경
    private String currentPassword;   // 비밀번호·이메일·휴대폰 변경 때 본인 확인
    private String newPassword;       // 비밀번호 변경
    private String email;             // 이메일 변경 1단계: 새 이메일
    private String code;              // 이메일 변경 2단계: 새 이메일로 받은 코드
    private String phone;             // 휴대폰 변경: 새 번호
    private String verificationToken; // 휴대폰 변경: /auth/phone/verify 로 받은 토큰
}
