package com.example.demo.service;

import com.example.demo.controller.dto.AccountResponse;
import com.example.demo.controller.dto.AccountUpdateRequest;
import com.example.demo.repository.EmailChangeRepository;
import com.example.demo.repository.MemberRepository;
import com.example.demo.repository.entity.EmailChange;
import com.example.demo.repository.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

// 마이페이지: 로그인한 회원이 자기 정보를 보고 고친다
// 비밀번호·이메일·휴대폰은 현재 비밀번호로 한 번 더 본인 확인을 한다 (켜 둔 화면을 남이 쓰는 경우 대비)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private static final String MAIL_PREFIX = "[Yuna's Bakery Lab] ";
    private static final int MAX_NAME_LENGTH = 50;

    private final MemberRepository memberRepository;
    private final EmailChangeRepository emailChangeRepository;
    private final MailService mailService;
    private final SmsService smsService;
    private final PhoneVerificationService phoneVerificationService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AccountResponse getAccount(Member member) {
        return new AccountResponse(member.getUsername(), member.getName(), member.getEmail(),
                Phones.format(member.getPhone()), smsService.isConfigured());
    }

    @Transactional
    public AccountResponse changeName(Member member, AccountUpdateRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름은 1~50자로 입력해주세요.");
        }
        member.setName(name);
        memberRepository.updateName(member);
        return getAccount(member);
    }

    @Transactional
    public void changePassword(Member member, AccountUpdateRequest request) {
        checkCurrentPassword(member, request);
        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "새 비밀번호는 4자 이상이어야 해요.");
        }
        if (passwordEncoder.matches(newPassword, member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지금 쓰는 비밀번호와 다른 비밀번호를 입력해주세요.");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.changePassword(member);
        // 본인이 바꾼 게 아니면 바로 알 수 있도록 알림 (메일 설정이 없거나 실패해도 변경은 유지)
        if (StringUtils.hasText(member.getEmail()) && mailService.isConfigured()) {
            mailService.trySend(member.getEmail(), MAIL_PREFIX + "비밀번호가 변경되었어요",
                    member.getName() + "님, 마이페이지에서 비밀번호가 변경되었습니다.\n\n"
                            + "본인이 변경하지 않았다면 즉시 비밀번호 찾기로 비밀번호를 다시 설정해 주세요.");
        }
    }

    // 이메일 변경 1단계: 현재 비밀번호 확인 → 새 이메일로 인증 코드 발송 (그 주소를 실제로 쓰는지 확인)
    @Transactional
    public void requestEmailChange(Member member, AccountUpdateRequest request) {
        checkCurrentPassword(member, request);
        String email = Emails.normalize(request.getEmail());
        if (!Emails.isValid(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일을 올바르게 입력해주세요.");
        }
        if (email.equals(member.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지금 쓰는 이메일과 같아요.");
        }
        if (memberRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }
        mailService.requireConfigured();
        if (emailChangeRepository.existsRecentByMemberId(member.getId())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "1분 뒤에 다시 받을 수 있어요.");
        }

        String code = String.format("%06d", random.nextInt(1_000_000));
        emailChangeRepository.deleteByMemberId(member.getId());
        emailChangeRepository.insertChange(EmailChange.createChange(member.getId(), email, passwordEncoder.encode(code)));
        boolean sent = mailService.trySend(email, MAIL_PREFIX + "이메일 변경 인증 코드",
                "인증 코드: " + code + "\n\n"
                        + "10분 안에 마이페이지에 입력하면 이 주소로 이메일이 바뀝니다.\n"
                        + "본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.");
        if (!sent) { // 예외로 롤백되어 1분 재발송 제한에도 걸리지 않는다
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }

    // 이메일 변경 2단계: 코드가 맞으면 바꾼다. 틀린 횟수를 남겨야 하므로 실패 응답에도 롤백하지 않는다
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public AccountResponse confirmEmailChange(Member member, AccountUpdateRequest request) {
        EmailChange change = emailChangeRepository.findValidByMemberId(member.getId());
        if (change == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었어요. 코드를 다시 받아주세요.");
        }
        String code = request.getCode() == null ? "" : request.getCode().trim();
        if (!passwordEncoder.matches(code, change.getCodeHash())) {
            emailChangeRepository.increaseAttempts(change.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않아요.");
        }
        if (memberRepository.existsByEmail(change.getEmail())) { // 코드를 받는 사이에 다른 회원이 그 이메일로 가입한 경우
            emailChangeRepository.deleteByMemberId(member.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
        }

        String oldEmail = member.getEmail();
        member.setEmail(change.getEmail());
        memberRepository.updateEmail(member);
        emailChangeRepository.deleteByMemberId(member.getId());
        if (StringUtils.hasText(oldEmail)) { // 예전 주소로도 알려서 남이 바꾼 경우 알아챌 수 있게
            mailService.trySend(oldEmail, MAIL_PREFIX + "이메일이 변경되었어요",
                    member.getName() + "님, 계정 이메일이 " + Emails.mask(change.getEmail()) + " 로 변경되었습니다.\n\n"
                            + "본인이 변경하지 않았다면 관리자에게 알려주세요.");
        }
        return getAccount(member);
    }

    // 휴대폰 변경: /auth/phone/request·verify 로 새 번호 인증을 마친 뒤 현재 비밀번호와 함께 요청
    @Transactional
    public AccountResponse changePhone(Member member, AccountUpdateRequest request) {
        if (!smsService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "지금은 휴대폰 번호를 바꿀 수 없어요.");
        }
        checkCurrentPassword(member, request);
        String phone = phoneVerificationService.consumeVerified(request.getPhone(), request.getVerificationToken());
        if (memberRepository.existsByPhone(phone)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 휴대폰 번호예요.");
        }
        member.setPhone(phone);
        memberRepository.updatePhone(member);
        return getAccount(member);
    }

    private void checkCurrentPassword(Member member, AccountUpdateRequest request) {
        if (!StringUtils.hasText(request.getCurrentPassword())
                || !passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않아요.");
        }
    }
}
