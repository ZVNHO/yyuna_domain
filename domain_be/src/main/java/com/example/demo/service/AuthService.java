package com.example.demo.service;

import com.example.demo.controller.dto.LoginRequest;
import com.example.demo.controller.dto.LoginResponse;
import com.example.demo.controller.dto.LoginVerifyRequest;
import com.example.demo.controller.dto.MemberResponse;
import com.example.demo.controller.dto.SignupRequest;
import com.example.demo.repository.LoginCodeRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.entity.LoginCode;
import com.example.demo.repository.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private static final String BEARER = "Bearer ";
    public static final int SESSION_MINUTES = 30; // 로그인 유지 시간. member.xml이 이 값을 그대로 쓴다

    private final MemberRepository memberRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final MailService mailService;
    private final SmsService smsService;
    private final PhoneVerificationService phoneVerificationService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public MemberResponse signup(SignupRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getPassword())
                || !StringUtils.hasText(request.getName()) || !StringUtils.hasText(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디, 비밀번호, 이름, 이메일을 모두 입력해주세요.");
        }
        String email = Emails.normalize(request.getEmail());
        if (!Emails.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 올바르게 입력해주세요.");
        }
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        Member member = Member.createMember(
                request.getUsername(), passwordEncoder.encode(request.getPassword()), request.getName(), email);
        // 문자 인증이 켜져 있으면 휴대폰 번호 인증을 마친 경우에만 가입된다
        if (smsService.isConfigured()) {
            member.setPhone(phoneVerificationService.consumeVerified(request.getPhone(), request.getPhoneVerificationToken()));
        }
        memberRepository.insertMember(member);
        return MemberResponse.responseMember(member);
    }

    public boolean isPhoneVerificationEnabled() {
        return smsService.isConfigured();
    }

    // 회원가입 화면의 실시간 중복 확인
    public boolean isUsernameAvailable(String username) {
        return StringUtils.hasText(username) && !memberRepository.existsByUsername(username.trim());
    }

    public boolean isEmailAvailable(String email) {
        String normalized = Emails.normalize(email);
        return Emails.isValid(normalized) && !memberRepository.existsByEmail(normalized);
    }

    // 1단계: 아이디·비밀번호 확인 → 휴대폰 번호가 있으면 문자로, 없으면 이메일로 인증 코드를 보내고 2단계로
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername());
        if (member == null || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        boolean bySms = StringUtils.hasText(member.getPhone()) && smsService.isConfigured();
        boolean byEmail = !bySms && StringUtils.hasText(member.getEmail()) && mailService.isConfigured();
        // 휴대폰·이메일이 없는 예전 회원이나 발송 설정 전에는 코드를 보낼 곳이 없어 바로 로그인
        if (!bySms && !byEmail) {
            return issueToken(member);
        }

        String channel = bySms ? "sms" : "email";
        String sentTo = bySms ? Phones.mask(member.getPhone()) : Emails.mask(member.getEmail());
        LoginCode recent = loginCodeRepository.findRecentByMemberId(member.getId());
        if (recent != null) { // 1분 안에 다시 로그인하면 또 보내지 않고 같은 코드로 진행
            return LoginResponse.responseTwoFactor(recent.getChallenge(), channel, sentTo);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        String challenge = UUID.randomUUID().toString();
        loginCodeRepository.deleteByMemberId(member.getId());
        loginCodeRepository.insertCode(LoginCode.createCode(member.getId(), challenge, passwordEncoder.encode(code)));
        boolean sent = bySms
                ? smsService.trySend(member.getPhone(),
                        "[유나의빵집] 로그인 인증번호 [" + code + "] (5분간 유효) 본인이 아니면 비밀번호를 변경하세요.")
                : mailService.trySend(member.getEmail(), "[Yuna's Bakery Lab] 로그인 인증 코드",
                        "로그인 인증 코드: " + code + "\n\n"
                                + "5분 안에 로그인 화면에 입력해 주세요.\n"
                                + "본인이 로그인하지 않았다면 비밀번호를 바꿔 주세요. 누군가 비밀번호를 알고 있다는 뜻이에요.");
        if (!sent) { // 예외로 트랜잭션이 롤백되어 저장한 코드도 사라진다
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    (bySms ? "인증 문자를" : "인증 메일을") + " 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
        return LoginResponse.responseTwoFactor(challenge, channel, sentTo);
    }

    // 2단계: 메일로 받은 코드 확인 후 로그인 토큰 발급. 틀린 횟수를 남겨야 하므로 실패 응답에도 롤백하지 않는다
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public LoginResponse verifyLoginCode(LoginVerifyRequest request) {
        if (!StringUtils.hasText(request.getChallenge()) || !StringUtils.hasText(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드를 입력해주세요.");
        }
        LoginCode loginCode = loginCodeRepository.findValidByChallenge(request.getChallenge());
        if (loginCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 만료됐어요. 다시 로그인해 주세요.");
        }
        if (!passwordEncoder.matches(request.getCode().trim(), loginCode.getCodeHash())) {
            loginCodeRepository.increaseAttempts(loginCode.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않아요.");
        }
        loginCodeRepository.deleteByMemberId(loginCode.getMemberId());
        return issueToken(memberRepository.findById(loginCode.getMemberId()));
    }

    private LoginResponse issueToken(Member member) {
        member.setToken(UUID.randomUUID().toString());
        memberRepository.updateToken(member);
        member.setSessionSecondsLeft((long) SESSION_MINUTES * 60);
        return LoginResponse.responseLogin(member);
    }

    // 「로그인을 연장하시겠습니까?」에서 연장을 누르면 지금부터 다시 30분
    @Transactional
    public Member extendSession(String authorization) {
        Member member = getLoginMember(authorization);
        memberRepository.extendToken(member.getId());
        member.setSessionSecondsLeft((long) SESSION_MINUTES * 60);
        return member;
    }

    @Transactional
    public void logout(String authorization) {
        Member member = getLoginMember(authorization);
        member.setToken(null);
        memberRepository.updateToken(member);
    }

    //Authorization: Bearer {token} 헤더로 로그인한 회원을 찾는다. 없으면 401
    public Member getLoginMember(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        Member member = memberRepository.findByToken(authorization.substring(BEARER.length()));
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다.");
        }
        return member;
    }
}
