package com.example.demo.service;

import com.example.demo.controller.dto.AccountResponse;
import com.example.demo.controller.dto.AccountUpdateRequest;
import com.example.demo.repository.EmailChangeRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.entity.EmailChange;
import com.example.demo.repository.entity.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final EmailChangeRepository emailChangeRepository = mock(EmailChangeRepository.class);
    private final MailService mailService = mock(MailService.class);
    private final SmsService smsService = mock(SmsService.class);
    private final PhoneVerificationService phoneVerificationService = mock(PhoneVerificationService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private AccountService service;
    private Member yuna;

    @BeforeEach
    void setUp() {
        service = new AccountService(memberRepository, emailChangeRepository, mailService, smsService, phoneVerificationService);
        yuna = Member.createMember("yuna", encoder.encode("pw1234"), "유나", "yuna0314@gmail.com");
        yuna.setId(7L);
        yuna.setPhone("01012345678");
        when(mailService.isConfigured()).thenReturn(true);
        when(mailService.trySend(any(), any(), any())).thenReturn(true);
        when(smsService.isConfigured()).thenReturn(true);
    }

    private AccountUpdateRequest request() {
        AccountUpdateRequest request = new AccountUpdateRequest();
        request.setCurrentPassword("pw1234");
        return request;
    }

    @Test
    void 내_정보는_휴대폰을_보기_좋게_보여주고_비밀번호는_없다() {
        AccountResponse res = service.getAccount(yuna);

        assertThat(res.getUsername()).isEqualTo("yuna");
        assertThat(res.getPhone()).isEqualTo("010-1234-5678");
        assertThat(res.isPhoneVerification()).isTrue();
    }

    @Test
    void 이름을_바꾼다() {
        AccountUpdateRequest req = request();
        req.setName("  빵순이 ");

        assertThat(service.changeName(yuna, req).getName()).isEqualTo("빵순이");
        verify(memberRepository).updateName(yuna);
    }

    @Test
    void 빈_이름은_안된다() {
        AccountUpdateRequest req = request();
        req.setName("  ");

        assertThatThrownBy(() -> service.changeName(yuna, req)).isInstanceOf(ResponseStatusException.class);
        verify(memberRepository, never()).updateName(any());
    }

    @Test
    void 현재_비밀번호가_맞으면_비밀번호를_바꾸고_알림_메일을_보낸다() {
        AccountUpdateRequest req = request();
        req.setNewPassword("newpw99");

        service.changePassword(yuna, req);

        assertThat(encoder.matches("newpw99", yuna.getPassword())).isTrue();
        verify(memberRepository).changePassword(yuna);
        verify(memberRepository, never()).updatePassword(any()); // 지금 로그인은 유지
        verify(mailService).trySend(eq("yuna0314@gmail.com"), contains("비밀번호가 변경"), any());
    }

    @Test
    void 현재_비밀번호가_틀리면_아무것도_바꾸지_않는다() {
        AccountUpdateRequest req = request();
        req.setCurrentPassword("wrong");
        req.setNewPassword("newpw99");

        assertThatThrownBy(() -> service.changePassword(yuna, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("현재 비밀번호");
        verify(memberRepository, never()).changePassword(any());
    }

    @Test
    void 새_이메일로_인증_코드를_보내고_코드가_맞으면_바꾼다() {
        AccountUpdateRequest req = request();
        req.setEmail(" New@Example.com ");

        service.requestEmailChange(yuna, req);

        ArgumentCaptor<EmailChange> saved = ArgumentCaptor.forClass(EmailChange.class);
        verify(emailChangeRepository).insertChange(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("new@example.com");
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailService).trySend(eq("new@example.com"), contains("이메일 변경"), body.capture());
        Matcher code = Pattern.compile("(\\d{6})").matcher(body.getValue());
        assertThat(code.find()).isTrue();

        saved.getValue().setId(3L);
        when(emailChangeRepository.findValidByMemberId(7L)).thenReturn(saved.getValue());
        AccountUpdateRequest confirm = new AccountUpdateRequest();
        confirm.setCode(code.group(1));

        AccountResponse res = service.confirmEmailChange(yuna, confirm);

        assertThat(res.getEmail()).isEqualTo("new@example.com");
        verify(memberRepository).updateEmail(yuna);
        verify(emailChangeRepository, atLeastOnce()).deleteByMemberId(7L);
        verify(mailService).trySend(eq("yuna0314@gmail.com"), contains("이메일이 변경"), any()); // 예전 주소로 알림
    }

    @Test
    void 이미_쓰는_이메일로는_바꿀_수_없다() {
        when(memberRepository.existsByEmail("taken@example.com")).thenReturn(true);
        AccountUpdateRequest req = request();
        req.setEmail("taken@example.com");

        assertThatThrownBy(() -> service.requestEmailChange(yuna, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 사용 중");
        verify(mailService, never()).trySend(any(), any(), any());
    }

    @Test
    void 이메일_인증_코드가_틀리면_횟수를_올리고_바꾸지_않는다() {
        EmailChange change = EmailChange.createChange(7L, "new@example.com", encoder.encode("123456"));
        change.setId(3L);
        when(emailChangeRepository.findValidByMemberId(7L)).thenReturn(change);
        AccountUpdateRequest confirm = new AccountUpdateRequest();
        confirm.setCode("999999");

        assertThatThrownBy(() -> service.confirmEmailChange(yuna, confirm)).isInstanceOf(ResponseStatusException.class);
        verify(emailChangeRepository).increaseAttempts(3L);
        verify(memberRepository, never()).updateEmail(any());
        assertThat(yuna.getEmail()).isEqualTo("yuna0314@gmail.com");
    }

    @Test
    void 문자_인증을_마친_새_번호로_휴대폰을_바꾼다() {
        when(phoneVerificationService.consumeVerified("010-9876-5432", "tok")).thenReturn("01098765432");
        AccountUpdateRequest req = request();
        req.setPhone("010-9876-5432");
        req.setVerificationToken("tok");

        assertThat(service.changePhone(yuna, req).getPhone()).isEqualTo("010-9876-5432");
        verify(memberRepository).updatePhone(yuna);
    }

    @Test
    void 휴대폰_변경도_현재_비밀번호가_필요하다() {
        AccountUpdateRequest req = request();
        req.setCurrentPassword("wrong");

        assertThatThrownBy(() -> service.changePhone(yuna, req)).isInstanceOf(ResponseStatusException.class);
        verify(phoneVerificationService, never()).consumeVerified(any(), any());
    }
}
