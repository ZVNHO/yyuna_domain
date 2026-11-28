package com.example.demo.repository;

import com.example.demo.repository.entity.PhoneVerification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 회원가입 휴대폰 번호 인증
@Mapper
public interface PhoneVerificationRepository {

    PhoneVerification findByPhone(String phone);

    int upsertCode(PhoneVerification verification); // 새 인증번호 저장 (3분 뒤 만료, 하루 발송 횟수 갱신)

    int increaseAttempts(long id);

    int markVerified(@Param("id") long id, @Param("verifiedToken") String verifiedToken); // 인증 완료 후 30분 안에 가입

    boolean existsVerified(@Param("phone") String phone, @Param("verifiedToken") String verifiedToken);

    int deleteByPhone(String phone);
}
