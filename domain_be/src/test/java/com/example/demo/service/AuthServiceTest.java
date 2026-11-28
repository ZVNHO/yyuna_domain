package com.example.demo.service;

import com.example.demo.controller.dto.LoginRequest;
import com.example.demo.controller.dto.LoginResponse;
import com.example.demo.controller.dto.LoginVerifyRequest;
import com.example.demo.repository.LoginCodeRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.entity.LoginCode;
import com.example.demo.repository.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final LoginCodeRepository loginCodeRepository = mock(LoginCodeRepository.class);
    private final MailService mailService = mock(MailService.class);
    private final SmsService smsService = mock(SmsService.class);
    private final PhoneVerificationService phoneVerificationService = mock(PhoneVerificationService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService service;
    private Member yuna;

    @BeforeEach
    void setUp() {
        service = new AuthService(memberRepository, loginCodeRepository, mailService, smsService, phoneVerificationService);
        yuna = Member.createMember("yuna", encoder.encode("pw1234"), "유나", "yuna0314@gmail.com");
        yuna.setId(7L);
        when(memberRepository.findByUsername("yuna")).thenReturn(yuna);
        when(memberRepository.findById(7L)).thenReturn(yuna);
        when(mailService.isConfigured()).thenReturn(true);
        when(mailService.trySend(any(), any(), any())).thenReturn(true);
    }

    @Test
    void 이메일이_있으면_토큰_대신_인증_코드를_메일로_보낸다() {
        LoginResponse res = service.login(login("yuna", "pw1234"));

        assertThat(res.isTwoFactorRequired()).isTrue();
        assertThat(res.getToken()).isNull();
        assertThat(res.getChannel()).isEqualTo("email");
        assertThat(res.getSentTo()).isEqualTo("yu******@gmail.com");

        ArgumentCaptor<LoginCode> saved = ArgumentCaptor.forClass(LoginCode.class);
        verify(loginCodeRepository).insertCode(saved.capture());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).trySend(eq("yuna0314@gmail.com"), contains("로그인 인증 코드"), text.capture());
        Matcher code = Pattern.compile("\\d{6}").matcher(text.getValue());
        assertThat(code.find()).isTrue();
        assertThat(saved.getValue().getChallenge()).isEqualTo(res.getChallenge());
        assertThat(encoder.matches(code.group(), saved.getValue().getCodeHash())).isTrue();
        verify(memberRepository, never()).updateToken(any());
    }

    @Test
    void 비밀번호가_틀리면_코드를_보내지_않는다() {
        assertThatThrownBy(() -> service.login(login("yuna", "wrong")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(loginCodeRepository);
        verify(mailService, never()).trySend(any(), any(), any());
    }

    @Test
    void 일분_안에_다시_로그인하면_메일을_또_보내지_않고_같은_코드로_진행() {
        when(loginCodeRepository.findRecentByMemberId(7L)).thenReturn(LoginCode.createCode(7L, "old-challenge", "hash"));

        LoginResponse res = service.login(login("yuna", "pw1234"));

        assertThat(res.getChallenge()).isEqualTo("old-challenge");
        verify(loginCodeRepository, never()).insertCode(any());
        verify(mailService, never()).trySend(any(), any(), any());
    }

    @Test
    void 메일을_못_보내면_503() {
        when(mailService.trySend(any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(login("yuna", "pw1234")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("인증 메일을 보내지 못했어요");
    }

    @Test
    void 이메일이_없는_예전_회원은_바로_로그인() {
        yuna.setEmail(null);

        LoginResponse res = service.login(login("yuna", "pw1234"));

        assertThat(res.isTwoFactorRequired()).isFalse();
        assertThat(res.getToken()).isNotBlank();
        verify(memberRepository).updateToken(yuna);
    }

    @Test
    void 코드가_맞으면_토큰을_발급하고_코드를_지운다() {
        LoginCode loginCode = LoginCode.createCode(7L, "challenge-1", encoder.encode("123456"));
        when(loginCodeRepository.findValidByChallenge("challenge-1")).thenReturn(loginCode);

        LoginResponse res = service.verifyLoginCode(verifyRequest("challenge-1", " 123456 "));

        assertThat(res.getToken()).isNotBlank();
        assertThat(res.getUsername()).isEqualTo("yuna");
        assertThat(res.getSessionSecondsLeft()).isEqualTo(30 * 60L);
        verify(loginCodeRepository).deleteByMemberId(7L);
        verify(memberRepository).updateToken(yuna);
    }

    @Test
    void 로그인_연장은_다시_30분() {
        yuna.setSessionSecondsLeft(45L);
        when(memberRepository.findByToken("tok")).thenReturn(yuna);

        Member extended = service.extendSession("Bearer tok");

        assertThat(extended.getSessionSecondsLeft()).isEqualTo(30 * 60L);
        verify(memberRepository).extendToken(7L);
    }

    @Test
    void 만료된_토큰이면_연장할_수_없다() {
        assertThatThrownBy(() -> service.extendSession("Bearer expired"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("로그인이 만료되었습니다");
        verify(memberRepository, never()).extendToken(anyLong());
    }

    @Test
    void 코드가_틀리면_틀린_횟수를_올리고_토큰은_없다() {
        LoginCode loginCode = LoginCode.createCode(7L, "challenge-1", encoder.encode("123456"));
        loginCode.setId(3L);
        when(loginCodeRepository.findValidByChallenge("challenge-1")).thenReturn(loginCode);

        assertThatThrownBy(() -> service.verifyLoginCode(verifyRequest("challenge-1", "999999")))
                .isInstanceOf(ResponseStatusException.class);
        verify(loginCodeRepository).increaseAttempts(3L);
        verify(memberRepository, never()).updateToken(any());
    }

    @Test
    void 만료되었거나_없는_challenge면_실패() {
        assertThatThrownBy(() -> service.verifyLoginCode(verifyRequest("nope", "123456")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("다시 로그인");
    }

    @Test
    void 휴대폰_번호가_있으면_이메일_대신_문자로_인증번호를_보낸다() {
        yuna.setPhone("01012345678");
        when(smsService.isConfigured()).thenReturn(true);
        when(smsService.trySend(any(), any())).thenReturn(true);

        LoginResponse res = service.login(login("yuna", "pw1234"));

        assertThat(res.isTwoFactorRequired()).isTrue();
        assertThat(res.getChannel()).isEqualTo("sms");
        assertThat(res.getSentTo()).isEqualTo("010-****-5678");
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsService).trySend(eq("01012345678"), text.capture());
        assertThat(text.getValue()).startsWith("[유나의빵집] 로그인 인증번호 [").matches(".*\\[\\d{6}].*");
        assertThat(text.getValue().getBytes(java.nio.charset.Charset.forName("EUC-KR")).length)
                .as("SMS 한 통(90바이트) 안에 들어가야 한다").isLessThanOrEqualTo(90);
        verify(mailService, never()).trySend(any(), any(), any());
    }

    @Test
    void 문자를_못_보내면_503() {
        yuna.setPhone("01012345678");
        when(smsService.isConfigured()).thenReturn(true);
        when(smsService.trySend(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.login(login("yuna", "pw1234")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("인증 문자를 보내지 못했어요");
    }

    @Test
    void 문자_설정이_없으면_휴대폰이_있어도_이메일로_보낸다() {
        yuna.setPhone("01012345678");

        LoginResponse res = service.login(login("yuna", "pw1234"));

        assertThat(res.getChannel()).isEqualTo("email");
        verify(smsService, never()).trySend(any(), any());
    }

    private static LoginRequest login(String username, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    private static LoginVerifyRequest verifyRequest(String challenge, String code) {
        LoginVerifyRequest request = new LoginVerifyRequest();
        ReflectionTestUtils.setField(request, "challenge", challenge);
        ReflectionTestUtils.setField(request, "code", code);
        return request;
    }
}
