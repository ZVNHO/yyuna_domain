package com.example.demo.controller;

import com.example.demo.controller.dto.*;
import com.example.demo.service.AccountRecoveryService;
import com.example.demo.service.AuthService;
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

    @PostMapping("/signup")//회원가입
    public MemberResponse signup(@RequestBody SignupRequest request) {
        return authService.signup(request);
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
