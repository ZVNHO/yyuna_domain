package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Member {
    private long id;
    private String username;
    private String password; // BCrypt로 암호화된 값
    private String name;
    private String email;    // 아이디·비밀번호 찾기용 (기존 회원은 null)
    private String phone;    // 문자 로그인 인증용, 숫자만 (기존 회원은 null)
    private String token;    // 로그인 시 발급되는 토큰
    private Long sessionSecondsLeft; // 로그인 만료까지 남은 초 (조회할 때만 계산, DB 컬럼 아님)

    public static Member createMember(String username, String password, String name, String email) {
        Member member = new Member();
        member.username = username;
        member.password = password;
        member.name = name;
        member.email = email;
        return member;
    }
}
