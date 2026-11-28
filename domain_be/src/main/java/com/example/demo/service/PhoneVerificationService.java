package com.example.demo.service;

import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PhoneVerificationRepository;
import com.example.demo.repository.entity.PhoneVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.UUID;

//회원가입 휴대폰 번호 인증: 인증번호 문자 발송 → 확인 → 회원가입 요청에서 확인 토큰 검사
//문자는 돈이 들어서 번호당 1분에 1번, 하루 5번까지만 보낸다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneVerificationService {

    private static final int MAX_ATTEMPTS = 5;

    private final PhoneVerificationRepository phoneVerificationRepository;
    private final MemberRepository memberRepository;
    private final SmsService smsService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public void sendCode(String rawPhone) {
        if (!smsService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "문자 인증이 아직 설정되지 않았어요. 관리자에게 문의해 주세요.");
        }
        String phone = requireValid(rawPhone);
        if (memberRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호예요.");
        }
        PhoneVerification existing = phoneVerificationRepository.findByPhone(phone);
        if (existing != null && existing.isRecentlySent()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "인증번호는 1분에 한 번만 받을 수 있어요. 잠시 후 다시 시도해 주세요.");
        }
        if (existing != null && existing.isDailyLimitReached()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "오늘은 인증번호를 더 받을 수 없어요. 내일 다시 시도해 주세요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        phoneVerificationRepository.upsertCode(PhoneVerification.createVerification(phone, passwordEncoder.encode(code)));
        boolean sent = smsService.trySend(phone, "[유나의빵집] 인증번호는 [" + code + "] 입니다. 3분 안에 입력해 주세요.");
        if (!sent) { // 예외로 트랜잭션이 롤백되어 발송 횟수도 늘지 않는다
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "인증번호 문자를 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    // 인증번호가 맞으면 회원가입 요청에 함께 보낼 확인 토큰을 돌려준다 (30분 유효)
    // 틀린 횟수를 남겨야 하므로 실패 응답에도 롤백하지 않는다
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String verifyCode(String rawPhone, String code) {
        String phone = requireValid(rawPhone);
        if (!StringUtils.hasText(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호를 입력해주세요.");
        }
        PhoneVerification verification = phoneVerificationRepository.findByPhone(phone);
        if (verification == null || verification.isExpired() || verification.getVerifiedToken() != null
                || verification.getAttempts() >= MAX_ATTEMPTS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 만료됐어요. 다시 받아주세요.");
        }
        if (!passwordEncoder.matches(code.trim(), verification.getCodeHash())) {
            phoneVerificationRepository.increaseAttempts(verification.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않아요.");
        }
        String token = UUID.randomUUID().toString();
        phoneVerificationRepository.markVerified(verification.getId(), token);
        return token;
    }

    // 회원가입: 인증을 마친 번호인지 확인하고, 다시 쓰지 못하게 지운다
    @Transactional
    public String consumeVerified(String rawPhone, String verifiedToken) {
        String phone = requireValid(rawPhone);
        if (!StringUtils.hasText(verifiedToken) || !phoneVerificationRepository.existsVerified(phone, verifiedToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "휴대폰 번호 인증을 먼저 완료해 주세요.");
        }
        phoneVerificationRepository.deleteByPhone(phone);
        return phone;
    }

    private static String requireValid(String rawPhone) {
        String phone = Phones.normalize(rawPhone);
        if (!Phones.isValid(phone)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "휴대폰 번호를 올바르게 입력해주세요. (예: 010-1234-5678)");
        }
        return phone;
    }
}
