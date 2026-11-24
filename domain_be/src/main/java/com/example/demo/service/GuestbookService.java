package com.example.demo.service;

import com.example.demo.controller.dto.GuestbookCreateRequest;
import com.example.demo.controller.dto.GuestbookReplyResponse;
import com.example.demo.controller.dto.GuestbookResponse;
import com.example.demo.repository.GuestbookRepository;
import com.example.demo.repository.entity.Guestbook;
import com.example.demo.repository.entity.GuestbookReply;
import com.example.demo.repository.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestbookService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final GuestbookRepository guestbookRepository;
    private final MailService mailService;

    public List<GuestbookResponse> getAll(Member loginMember) {
        Map<Long, List<GuestbookReplyResponse>> replies = guestbookRepository.findAllReplies().stream()
                .map(entity -> GuestbookReplyResponse.responseReply(entity, loginMember.getId()))
                .collect(Collectors.groupingBy(GuestbookReplyResponse::getGuestbookId));

        return guestbookRepository.findAll().stream()
                .map(entity -> GuestbookResponse.responseGuestbook(
                        entity, loginMember.getId(), replies.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void create(Member loginMember, GuestbookCreateRequest request) {
        validateContent(request.getContent());
        guestbookRepository.insertGuestbook(Guestbook.createGuestbook(loginMember.getId(), request.getContent()));

        // 관리자가 매번 들어와 확인하지 않아도 되도록 새 글을 메일로 알린다 (관리자 본인 글은 제외)
        if (!mailService.isAdmin(loginMember.getEmail())) {
            mailService.notifyAdmin("[Yuna's Bakery Lab] 방명록 새 글 - " + loginMember.getName(),
                    loginMember.getName() + "(" + loginMember.getUsername() + ")님이 방명록에 글을 남겼어요.\n\n"
                            + request.getContent() + "\n\n"
                            + "http://yyuna03.cafe24.com 에서 확인하세요.");
        }
    }

    @Transactional
    public void update(Member loginMember, long id, GuestbookCreateRequest request) {
        validateContent(request.getContent());
        Guestbook guestbook = guestbookRepository.findById(id);
        if (guestbook == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "글이 없습니다.");
        }
        if (guestbook.getMemberId() != loginMember.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 쓴 글만 수정할 수 있습니다.");
        }
        guestbook.setContent(request.getContent());
        guestbookRepository.updateGuestbook(guestbook);
    }

    @Transactional
    public void delete(Member loginMember, long id) {
        Guestbook guestbook = guestbookRepository.findById(id);
        if (guestbook == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "글이 없습니다.");
        }
        if (guestbook.getMemberId() != loginMember.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 쓴 글만 삭제할 수 있습니다.");
        }
        guestbookRepository.deleteById(id);
    }

    // parentReplyId가 있으면 그 답글에 다는 답글 (같은 방명록 글의 답글이어야 한다)
    @Transactional
    public void createReply(Member loginMember, long guestbookId, GuestbookCreateRequest request) {
        validateContent(request.getContent());
        if (guestbookRepository.findById(guestbookId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "글이 없습니다.");
        }
        Long parentReplyId = request.getParentReplyId();
        if (parentReplyId != null) {
            GuestbookReply parent = guestbookRepository.findReplyById(parentReplyId);
            if (parent == null || parent.getGuestbookId() != guestbookId) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "답글을 달 원래 답글이 없습니다.");
            }
        }
        guestbookRepository.insertReply(
                GuestbookReply.createReply(guestbookId, parentReplyId, loginMember.getId(), request.getContent()));
    }

    @Transactional
    public void updateReply(Member loginMember, long replyId, GuestbookCreateRequest request) {
        validateContent(request.getContent());
        GuestbookReply reply = guestbookRepository.findReplyById(replyId);
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "답글이 없습니다.");
        }
        if (reply.getMemberId() != loginMember.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 쓴 답글만 수정할 수 있습니다.");
        }
        reply.setContent(request.getContent());
        guestbookRepository.updateReply(reply);
    }

    @Transactional
    public void deleteReply(Member loginMember, long replyId) {
        GuestbookReply reply = guestbookRepository.findReplyById(replyId);
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "답글이 없습니다.");
        }
        if (reply.getMemberId() != loginMember.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 쓴 답글만 삭제할 수 있습니다.");
        }
        guestbookRepository.deleteReplyById(replyId);
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용은 500자까지 입력할 수 있습니다.");
        }
    }
}
