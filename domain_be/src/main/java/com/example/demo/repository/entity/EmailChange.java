package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailChange {
    private long id;
    private long memberId;
    private String email;    // 바꾸려는 새 이메일
    private String codeHash; // BCrypt로 암호화된 6자리 인증 코드
    private int attempts;    // 틀린 횟수

    public static EmailChange createChange(long memberId, String email, String codeHash) {
        EmailChange change = new EmailChange();
        change.memberId = memberId;
        change.email = email;
        change.codeHash = codeHash;
        return change;
    }
}
