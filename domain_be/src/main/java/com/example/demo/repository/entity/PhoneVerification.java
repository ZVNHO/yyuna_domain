package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneVerification {
    private long id;
    private String phone;
    private String codeHash;          // BCrypt로 암호화된 6자리 인증번호
    private int attempts;             // 틀린 횟수
    private String verifiedToken;     // 인증을 마치면 발급
    // 아래는 조회할 때 DB의 NOW() 기준으로 계산한 값
    private boolean recentlySent;     // 1분 안에 보냈는지
    private boolean dailyLimitReached; // 하루 5번을 다 보냈는지
    private boolean expired;          // 인증번호 유효시간이 지났는지

    public static PhoneVerification createVerification(String phone, String codeHash) {
        PhoneVerification verification = new PhoneVerification();
        verification.phone = phone;
        verification.codeHash = codeHash;
        return verification;
    }
}
