package com.example.demo.service;

import com.example.demo.controller.dto.FindUsernameRequest;
import com.example.demo.controller.dto.PasswordResetConfirmRequest;
import com.example.demo.controller.dto.PasswordResetRequest;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PasswordResetRepository;
import com.example.demo.repository.entity.Member;
import com.example.demo.repository.entity.PasswordReset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountRecoveryServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final PasswordResetRepository passwordResetRepository = mock(PasswordResetRepository.class);
    private final MailService mailService = mock(MailService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private AccountRecoveryService service;
    private Member yuna;

    @BeforeEach
    void setUp() {
        service = new AccountRecoveryService(memberRepository, passwordResetRepository, mailService);
        yuna = Member.createMember("yuna", encoder.encode("old-pw"), "유나", "yuna@example.com");
        yuna.setId(7L);
        when(memberRepository.findByUsername("yuna")).thenReturn(yuna);
        when(memberRepository.findByEmail("yuna@example.com")).thenReturn(yuna);
    }

    @Test
    void 아이디_찾기는_이메일을_정리해서_찾고_아이디를_메일로_보낸다() {
        service.sendUsername(request(new FindUsernameRequest(), "email", "  YUNA@Example.com "));

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("yuna@example.com"), contains("아이디"), text.capture());
        assertThat(text.getValue()).contains("yuna");
    }

    @Test
    void 없는_이메일이면_메일을_보내지_않지만_에러도_내지_않는다() {
        service.sendUsername(request(new FindUsernameRequest(), "email", "nobody@example.com"));

        verify(mailService, never()).send(any(), any(), any());
    }

    @Test
    void 메일_설정이_없으면_회원을_조회하기_전에_503() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(mailService).requireConfigured();

        assertThatThrownBy(() -> service.sendUsername(request(new FindUsernameRequest(), "email", "yuna@example.com")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(memberRepository);
    }

    @Test
    void 아이디와_이메일이_맞으면_암호화된_코드를_저장하고_원래_코드를_메일로_보낸다() {
        service.sendResetCode(resetRequest("yuna", "yuna@example.com"));

        ArgumentCaptor<PasswordReset> saved = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).deleteByMemberId(7L);
        verify(passwordResetRepository).insertReset(saved.capture());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq("yuna@example.com"), contains("인증 코드"), text.capture());

        Matcher code = Pattern.compile("\\d{6}").matcher(text.getValue());
        assertThat(code.find()).isTrue();
        assertThat(saved.getValue().getMemberId()).isEqualTo(7L);
        assertThat(saved.getValue().getCodeHash()).isNotEqualTo(code.group());
        assertThat(encoder.matches(code.group(), saved.getValue().getCodeHash())).isTrue();
    }

    @Test
    void 이메일이_다르면_코드를_만들지_않는다() {
        service.sendResetCode(resetRequest("yuna", "other@example.com"));

        verify(passwordResetRepository, never()).insertReset(any());
        verify(mailService, never()).send(any(), any(), any());
    }

    @Test
    void 일분_안에_다시_요청하면_코드를_새로_보내지_않는다() {
        when(passwordResetRepository.existsRecentByMemberId(7L)).thenReturn(true);

        service.sendResetCode(resetRequest("yuna", "yuna@example.com"));

        verify(passwordResetRepository, never()).insertReset(any());
        verify(mailService, never()).send(any(), any(), any());
    }

    @Test
    void 코드가_틀리면_틀린_횟수를_올리고_비밀번호는_그대로() {
        PasswordReset reset = PasswordReset.createReset(7L, encoder.encode("123456"));
        reset.setId(3L);
        when(passwordResetRepository.findValidByMemberId(7L)).thenReturn(reset);

        assertThatThrownBy(() -> service.resetPassword(confirmRequest("yuna", "999999", "new-pw")))
                .isInstanceOf(ResponseStatusException.class);
        verify(passwordResetRepository).increaseAttempts(3L);
        verify(memberRepository, never()).updatePassword(any());
    }

    @Test
    void 코드가_맞으면_새_비밀번호를_암호화해_저장하고_코드를_지운다() {
        when(passwordResetRepository.findValidByMemberId(7L))
                .thenReturn(PasswordReset.createReset(7L, encoder.encode("123456")));

        service.resetPassword(confirmRequest("yuna", " 123456 ", "new-pw"));

        verify(memberRepository).updatePassword(yuna);
        assertThat(encoder.matches("new-pw", yuna.getPassword())).isTrue();
        verify(passwordResetRepository).deleteByMemberId(7L);
    }

    @Test
    void 유효한_코드가_없으면_실패() {
        assertThatThrownBy(() -> service.resetPassword(confirmRequest("yuna", "123456", "new-pw")))
                .isInstanceOf(ResponseStatusException.class);
        verify(memberRepository, never()).updatePassword(any());
    }

    @Test
    void 새_비밀번호가_4자_미만이면_실패() {
        assertThatThrownBy(() -> service.resetPassword(confirmRequest("yuna", "123456", "abc")))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(passwordResetRepository);
    }

    private PasswordResetRequest resetRequest(String username, String email) {
        PasswordResetRequest request = request(new PasswordResetRequest(), "username", username);
        return request(request, "email", email);
    }

    private PasswordResetConfirmRequest confirmRequest(String username, String code, String newPassword) {
        PasswordResetConfirmRequest request = request(new PasswordResetConfirmRequest(), "username", username);
        request(request, "code", code);
        return request(request, "newPassword", newPassword);
    }

    // DTO에는 setter가 없어서(JSON으로만 채워짐) 테스트에서는 필드에 직접 넣는다
    private static <T> T request(T dto, String field, String value) {
        ReflectionTestUtils.setField(dto, field, value);
        return dto;
    }
}
