package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordReset {
    private long id;
    private long memberId;
    private String codeHash; // BCrypt로 암호화된 6자리 인증 코드
    private int attempts;    // 틀린 횟수

    public static PasswordReset createReset(long memberId, String codeHash) {
        PasswordReset reset = new PasswordReset();
        reset.memberId = memberId;
        reset.codeHash = codeHash;
        return reset;
    }
}
