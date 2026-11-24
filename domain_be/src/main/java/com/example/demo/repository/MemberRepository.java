package com.example.demo.repository;

import com.example.demo.repository.entity.Member;
import org.apache.ibatis.annotations.Mapper;
//로그인 회원가입
@Mapper
public interface MemberRepository {

    int insertMember(Member member);

    boolean existsByUsername(String username);//회원가입

    Member findByUsername(String username);// 로그인

    Member findByToken(String token);//│ 로그인 확인

    Member findById(long id);// 로그인 2단계 인증

    int updateToken(Member member);//로그아웃

    boolean existsByEmail(String email);// 회원가입 이메일 중복 확인

    Member findByEmail(String email);// 아이디 찾기

    int updatePassword(Member member);// 비밀번호 재설정 (로그인 토큰도 지움)
}
