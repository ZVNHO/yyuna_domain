package com.example.demo.service;

import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PhoneVerificationRepository;
import com.example.demo.repository.entity.PhoneVerification;
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

class PhoneVerificationServiceTest {

    private final PhoneVerificationRepository repository = mock(PhoneVerificationRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final SmsService smsService = mock(SmsService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private PhoneVerificationService service;

    @BeforeEach
    void setUp() {
        service = new PhoneVerificationService(repository, memberRepository, smsService);
        when(smsService.isConfigured()).thenReturn(true);
        when(smsService.trySend(any(), any())).thenReturn(true);
    }

    @Test
    void 인증번호를_암호화해_저장하고_원래_번호를_문자로_보낸다() {
        service.sendCode("010-1234-5678");

        ArgumentCaptor<PhoneVerification> saved = ArgumentCaptor.forClass(PhoneVerification.class);
        verify(repository).upsertCode(saved.capture());
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(smsService).trySend(eq("01012345678"), text.capture());
        Matcher code = Pattern.compile("\\d{6}").matcher(text.getValue());
        assertThat(code.find()).isTrue();
        assertThat(saved.getValue().getPhone()).isEqualTo("01012345678");
        assertThat(encoder.matches(code.group(), saved.getValue().getCodeHash())).isTrue();
    }

    @Test
    void 일분_안에_다시_요청하거나_하루_5번을_넘기면_보내지_않는다() {
        PhoneVerification recent = PhoneVerification.createVerification("01012345678", "hash");
        recent.setRecentlySent(true);
        when(repository.findByPhone("01012345678")).thenReturn(recent);
        assertThatThrownBy(() -> service.sendCode("01012345678")).hasMessageContaining("1분에 한 번");

        recent.setRecentlySent(false);
        recent.setDailyLimitReached(true);
        assertThatThrownBy(() -> service.sendCode("01012345678")).hasMessageContaining("오늘은");

        verify(smsService, never()).trySend(any(), any());
    }

    @Test
    void 이미_가입된_번호나_잘못된_번호는_거부() {
        when(memberRepository.existsByPhone("01012345678")).thenReturn(true);
        assertThatThrownBy(() -> service.sendCode("01012345678")).hasMessageContaining("이미 가입된");
        assertThatThrownBy(() -> service.sendCode("02-123-4567")).hasMessageContaining("올바르게");
        verify(smsService, never()).trySend(any(), any());
    }

    @Test
    void 인증번호가_맞으면_확인_토큰을_발급하고_틀리면_횟수를_올린다() {
        PhoneVerification verification = PhoneVerification.createVerification("01012345678", encoder.encode("123456"));
        verification.setId(9L);
        when(repository.findByPhone("01012345678")).thenReturn(verification);

        assertThatThrownBy(() -> service.verifyCode("01012345678", "999999")).isInstanceOf(ResponseStatusException.class);
        verify(repository).increaseAttempts(9L);

        String token = service.verifyCode("010-1234-5678", "123456");
        assertThat(token).isNotBlank();
        verify(repository).markVerified(9L, token);
    }

    @Test
    void 만료된_인증번호는_맞아도_실패() {
        PhoneVerification verification = PhoneVerification.createVerification("01012345678", encoder.encode("123456"));
        verification.setExpired(true);
        when(repository.findByPhone("01012345678")).thenReturn(verification);

        assertThatThrownBy(() -> service.verifyCode("01012345678", "123456")).hasMessageContaining("만료");
        verify(repository, never()).markVerified(anyLong(), any());
    }

    @Test
    void 회원가입은_인증을_마친_번호만_통과하고_한_번_쓰면_지운다() {
        when(repository.existsVerified("01012345678", "tok")).thenReturn(true);

        assertThat(service.consumeVerified("010-1234-5678", "tok")).isEqualTo("01012345678");
        verify(repository).deleteByPhone("01012345678");

        assertThatThrownBy(() -> service.consumeVerified("01012345678", "wrong")).hasMessageContaining("인증을 먼저");
    }
}
