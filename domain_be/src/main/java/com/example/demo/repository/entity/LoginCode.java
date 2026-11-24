package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginCode {
    private long id;
    private long memberId;
    private String challenge; // 1단계(비밀번호) 통과 표시. 2단계 요청에서 회원을 찾는 데 쓴다
    private String codeHash;  // BCrypt로 암호화된 6자리 인증 코드
    private int attempts;     // 틀린 횟수

    public static LoginCode createCode(long memberId, String challenge, String codeHash) {
        LoginCode loginCode = new LoginCode();
        loginCode.memberId = memberId;
        loginCode.challenge = challenge;
        loginCode.codeHash = codeHash;
        return loginCode;
    }
}
