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

    int updateToken(Member member);// 로그인(30분 만료 시각도 함께 저장)·로그아웃

    int extendToken(long id);// 로그인 연장: 지금부터 다시 30분

    boolean existsByEmail(String email);// 회원가입 이메일 중복 확인

    boolean existsByPhone(String phone);// 회원가입 휴대폰 번호 중복 확인

    Member findByEmail(String email);// 아이디 찾기

    int updatePassword(Member member);// 비밀번호 재설정 (로그인 토큰도 지움)
}
