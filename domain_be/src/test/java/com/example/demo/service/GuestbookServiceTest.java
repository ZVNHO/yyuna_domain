package com.example.demo.service;

import com.example.demo.controller.dto.GuestbookCreateRequest;
import com.example.demo.repository.GuestbookRepository;
import com.example.demo.repository.entity.Guestbook;
import com.example.demo.repository.entity.GuestbookReply;
import com.example.demo.repository.entity.Member;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuestbookServiceTest {

    private final GuestbookRepository guestbookRepository = mock(GuestbookRepository.class);
    private final MailService mailService = mock(MailService.class);
    private final GuestbookService service = new GuestbookService(guestbookRepository, mailService);

    @Test
    void 방명록에_새_글이_올라오면_관리자에게_알림_메일() {
        Member visitor = Member.createMember("jiwoo", "hash", "송지우", "jiwoo@example.com");

        service.create(visitor, content("안녕하세요!"));

        verify(guestbookRepository).insertGuestbook(any());
        verify(mailService).notifyAdmin(contains("송지우"), contains("안녕하세요!"));
    }

    @Test
    void 관리자_본인이_쓴_글은_알리지_않는다() {
        Member admin = Member.createMember("yuna", "hash", "유나", "fromdajung@naver.com");
        when(mailService.isAdmin("fromdajung@naver.com")).thenReturn(true);

        service.create(admin, content("공지"));

        verify(mailService, never()).notifyAdmin(any(), any());
    }

    @Test
    void 본인_글은_수정되고_남의_글은_403() {
        Member writer = member(1L);
        Guestbook entry = Guestbook.createGuestbook(1L, "처음 글");
        entry.setId(10L);
        when(guestbookRepository.findById(10L)).thenReturn(entry);

        service.update(writer, 10L, content("고친 글"));
        verify(guestbookRepository).updateGuestbook(entry);
        assertThat(entry.getContent()).isEqualTo("고친 글");

        assertThatThrownBy(() -> service.update(member(2L), 10L, content("남의 글 수정")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("본인이 쓴 글만 수정");
    }

    @Test
    void 답글에_다시_답글을_달면_원래_답글_id가_저장된다() {
        when(guestbookRepository.findById(10L)).thenReturn(Guestbook.createGuestbook(1L, "글"));
        when(guestbookRepository.findReplyById(5L)).thenReturn(GuestbookReply.createReply(10L, null, 1L, "답글"));

        service.createReply(member(2L), 10L, content("답글의 답글", 5L));

        ArgumentCaptor<GuestbookReply> saved = ArgumentCaptor.forClass(GuestbookReply.class);
        verify(guestbookRepository).insertReply(saved.capture());
        assertThat(saved.getValue().getParentReplyId()).isEqualTo(5L);
        assertThat(saved.getValue().getGuestbookId()).isEqualTo(10L);
    }

    @Test
    void 다른_방명록_글의_답글에는_답글을_달_수_없다() {
        when(guestbookRepository.findById(10L)).thenReturn(Guestbook.createGuestbook(1L, "글"));
        when(guestbookRepository.findReplyById(5L)).thenReturn(GuestbookReply.createReply(99L, null, 1L, "다른 글의 답글"));

        assertThatThrownBy(() -> service.createReply(member(2L), 10L, content("답글", 5L)))
                .isInstanceOf(ResponseStatusException.class);
        verify(guestbookRepository, never()).insertReply(any());
    }

    @Test
    void 본인_답글만_수정할_수_있다() {
        GuestbookReply reply = GuestbookReply.createReply(10L, null, 1L, "답글");
        when(guestbookRepository.findReplyById(5L)).thenReturn(reply);

        assertThatThrownBy(() -> service.updateReply(member(2L), 5L, content("남의 답글 수정")))
                .isInstanceOf(ResponseStatusException.class);
        verify(guestbookRepository, never()).updateReply(any());

        service.updateReply(member(1L), 5L, content("고친 답글"));
        verify(guestbookRepository).updateReply(reply);
    }

    private static Member member(long id) {
        Member member = Member.createMember("user" + id, "hash", "회원" + id, null);
        member.setId(id);
        return member;
    }

    private static GuestbookCreateRequest content(String text) {
        return content(text, null);
    }

    private static GuestbookCreateRequest content(String text, Long parentReplyId) {
        GuestbookCreateRequest request = new GuestbookCreateRequest();
        ReflectionTestUtils.setField(request, "content", text);
        ReflectionTestUtils.setField(request, "parentReplyId", parentReplyId);
        return request;
    }
}
