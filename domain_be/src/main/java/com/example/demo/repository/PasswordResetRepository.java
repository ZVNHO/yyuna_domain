package com.example.demo.repository;

import com.example.demo.repository.entity.PasswordReset;
import org.apache.ibatis.annotations.Mapper;

// 비밀번호 재설정 인증 코드
@Mapper
public interface PasswordResetRepository {

    int insertReset(PasswordReset reset); // 10분 뒤 만료

    int deleteByMemberId(long memberId);

    boolean existsRecentByMemberId(long memberId); // 1분 안에 보낸 코드가 있는지 (재발송 제한)

    PasswordReset findValidByMemberId(long memberId); // 만료 전이고 5번 미만으로 틀린 코드

    int increaseAttempts(long id);
}
