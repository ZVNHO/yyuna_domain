package com.example.demo.controller;

import com.example.demo.controller.dto.*;
import com.example.demo.service.AccountRecoveryService;
import com.example.demo.service.AuthService;
import com.example.demo.service.PhoneVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

//회원가입 / 로그인 / 로그아웃 / 내 정보
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final String RECOVERY_SENT = "입력한 정보가 맞으면 메일을 보냈어요. 메일함(스팸함 포함)을 확인해 주세요.";

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/signup")//회원가입
    public MemberResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @GetMapping("/signup-options")// 회원가입: 휴대폰 문자 인증을 받는지
    public SignupOptionsResponse signupOptions() {
        return SignupOptionsResponse.createResponse(authService.isPhoneVerificationEnabled());
    }

    @PostMapping("/phone/request")// 회원가입: 휴대폰으로 인증번호 문자 발송
    public MessageResponse requestPhoneCode(@RequestBody PhoneCodeRequest request) {
        phoneVerificationService.sendCode(request.getPhone());
        return MessageResponse.createResponse("인증번호를 보냈어요. 3분 안에 입력해 주세요.");
    }

    @PostMapping("/phone/verify")// 회원가입: 인증번호 확인 → 가입 요청에 쓸 확인 토큰
    public PhoneVerifyResponse verifyPhoneCode(@RequestBody PhoneCodeRequest request) {
        return PhoneVerifyResponse.createResponse(phoneVerificationService.verifyCode(request.getPhone(), request.getCode()));
    }

    @GetMapping("/check-username")// 회원가입: 아이디 중복 확인
    public AvailabilityResponse checkUsername(@RequestParam String username) {
        return AvailabilityResponse.createResponse(authService.isUsernameAvailable(username));
    }

    @GetMapping("/check-email")// 회원가입: 이메일 중복 확인
    public AvailabilityResponse checkEmail(@RequestParam String email) {
        return AvailabilityResponse.createResponse(authService.isEmailAvailable(email));
    }

    @PostMapping("/login")//로그인
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/login/verify")// 로그인 2단계: 이메일로 받은 인증 코드 확인
    public LoginResponse verifyLogin(@RequestBody LoginVerifyRequest request) {
        return authService.verifyLoginCode(request);
    }

    @PostMapping("/logout")//로그아웃
    public MessageResponse logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(authorization);
        return MessageResponse.createResponse("로그아웃 되었습니다.");
    }

    @GetMapping("/me")// 내 정보
    public MemberResponse me(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return MemberResponse.responseMember(authService.getLoginMember(authorization));
    }

    @PostMapping("/session/extend")// 로그인 연장: 지금부터 다시 30분
    public MemberResponse extendSession(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return MemberResponse.responseMember(authService.extendSession(authorization));
    }

    @PostMapping("/find-username")// 아이디 찾기: 이메일로 아이디를 보내준다
    public MessageResponse findUsername(@RequestBody FindUsernameRequest request) {
        accountRecoveryService.sendUsername(request);
        return MessageResponse.createResponse(RECOVERY_SENT);
    }

    @PostMapping("/password-reset/request")// 비밀번호 찾기 1단계: 인증 코드 메일 발송
    public MessageResponse requestPasswordReset(@RequestBody PasswordResetRequest request) {
        accountRecoveryService.sendResetCode(request);
        return MessageResponse.createResponse(RECOVERY_SENT);
    }

    @PostMapping("/password-reset/confirm")// 비밀번호 찾기 2단계: 코드 확인 후 새 비밀번호로 변경
    public MessageResponse confirmPasswordReset(@RequestBody PasswordResetConfirmRequest request) {
        accountRecoveryService.resetPassword(request);
        return MessageResponse.createResponse("비밀번호를 바꿨어요. 새 비밀번호로 로그인해 주세요.");
    }
}
