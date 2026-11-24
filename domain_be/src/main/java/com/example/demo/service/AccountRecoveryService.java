package com.example.demo.service;

import com.example.demo.controller.dto.FindUsernameRequest;
import com.example.demo.controller.dto.PasswordResetConfirmRequest;
import com.example.demo.controller.dto.PasswordResetRequest;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.PasswordResetRepository;
import com.example.demo.repository.entity.Member;
import com.example.demo.repository.entity.PasswordReset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

//아이디 찾기 / 비밀번호 재설정
//가입 여부가 드러나지 않도록, 회원이 없거나 정보가 틀려도 요청 단계는 항상 같은 응답으로 끝난다
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountRecoveryService {

    private static final String MAIL_PREFIX = "[Yuna's Bakery Lab] ";

    private final MemberRepository memberRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final MailService mailService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public void sendUsername(FindUsernameRequest request) {
        String email = Emails.normalize(request.getEmail());
        if (!Emails.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 올바르게 입력해주세요.");
        }
        mailService.requireConfigured();

        Member member = memberRepository.findByEmail(email);
        if (member != null) {
            mailService.send(email, MAIL_PREFIX + "아이디 안내",
                    member.getName() + "님의 아이디는 " + member.getUsername() + " 입니다.\n\n"
                            + "본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.");
        }
    }

    @Transactional
    public void sendResetCode(PasswordResetRequest request) {
        String email = Emails.normalize(request.getEmail());
        if (!StringUtils.hasText(request.getUsername()) || !Emails.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디와 이메일을 올바르게 입력해주세요.");
        }
        mailService.requireConfigured();

        Member member = memberRepository.findByUsername(request.getUsername().trim());
        if (member == null || !email.equals(member.getEmail())) return;
        if (passwordResetRepository.existsRecentByMemberId(member.getId())) return; // 1분 안 재발송 막기

        String code = String.format("%06d", random.nextInt(1_000_000));
        passwordResetRepository.deleteByMemberId(member.getId());
        passwordResetRepository.insertReset(PasswordReset.createReset(member.getId(), passwordEncoder.encode(code)));
        mailService.send(email, MAIL_PREFIX + "비밀번호 재설정 인증 코드",
                "인증 코드: " + code + "\n\n"
                        + "10분 안에 비밀번호 재설정 화면에 입력해 주세요.\n"
                        + "본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.");
    }

    // 틀린 횟수를 남겨야 하므로 실패 응답(ResponseStatusException)에도 롤백하지 않는다
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void resetPassword(PasswordResetConfirmRequest request) {
        if (!StringUtils.hasText(request.getUsername()) || !StringUtils.hasText(request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디와 인증 코드를 입력해주세요.");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호는 4자 이상이어야 해요.");
        }

        Member member = memberRepository.findByUsername(request.getUsername().trim());
        PasswordReset reset = member == null ? null : passwordResetRepository.findValidByMemberId(member.getId());
        if (reset == null) {
            throw invalidCode();
        }
        if (!passwordEncoder.matches(request.getCode().trim(), reset.getCodeHash())) {
            passwordResetRepository.increaseAttempts(reset.getId());
            throw invalidCode();
        }

        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        memberRepository.updatePassword(member);
        passwordResetRepository.deleteByMemberId(member.getId());
    }

    private ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었어요. 코드를 다시 받아주세요.");
    }
}
