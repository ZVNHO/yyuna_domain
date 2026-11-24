package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

//메일 발송 (네이버 SMTP, 계정은 secret.yaml의 spring.mail.username/password)
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${app.mail.admin:}")
    private String admin;

    // secret.yaml에 SMTP 비밀번호가 들어 있어야 메일을 보낼 수 있다
    public boolean isConfigured() {
        return StringUtils.hasText(smtpPassword) && StringUtils.hasText(from);
    }

    // 회원 조회 전에 확인해서, 설정이 없을 때의 에러가 가입 여부를 드러내지 않게 한다
    public void requireConfigured() {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "메일 발송이 아직 설정되지 않았어요. 관리자에게 문의해 주세요.");
        }
    }

    // 실패해도 예외를 던지지 않는다: 응답이 달라지면 가입된 이메일인지 알 수 있기 때문 (찾기 기능용)
    public void send(String to, String subject, String text) {
        trySend(to, subject, text);
    }

    // 보냈는지 결과가 필요할 때 (로그인 인증 코드: 못 보냈으면 사용자에게 알려야 한다)
    public boolean trySend(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            mailSender.send(message);
            return true;
        } catch (MailException e) {
            log.error("메일 발송 실패: to={}", to, e);
            return false;
        }
    }

    // 관리자 알림은 응답을 늦추지 않도록 백그라운드에서 보낸다
    @Async
    public void notifyAdmin(String subject, String text) {
        if (!isConfigured() || !StringUtils.hasText(admin)) return;
        trySend(admin, subject, text);
    }

    public boolean isAdmin(String email) {
        return StringUtils.hasText(email) && email.equalsIgnoreCase(admin);
    }
}
