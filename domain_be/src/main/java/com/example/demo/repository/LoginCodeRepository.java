package com.example.demo.repository;

import com.example.demo.repository.entity.LoginCode;
import org.apache.ibatis.annotations.Mapper;

// 로그인 2단계 인증 코드
@Mapper
public interface LoginCodeRepository {

    int insertCode(LoginCode loginCode); // 5분 뒤 만료

    int deleteByMemberId(long memberId);

    LoginCode findRecentByMemberId(long memberId); // 1분 안에 보낸 코드 (재발송 제한)

    LoginCode findValidByChallenge(String challenge); // 만료 전이고 5번 미만으로 틀린 코드

    int increaseAttempts(long id);
}
