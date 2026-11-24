package com.example.demo.controller;

import com.example.demo.controller.dto.GuestbookCreateRequest;
import com.example.demo.controller.dto.GuestbookResponse;
import com.example.demo.controller.dto.MessageResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.GuestbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//방명록 (로그인한 회원만 사용)
@RestController
@RequestMapping("/guestbook")
@RequiredArgsConstructor
public class GuestbookController {
    private final AuthService authService;
    private final GuestbookService guestbookService;

    @GetMapping()
    public List<GuestbookResponse> list(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return guestbookService.getAll(authService.getLoginMember(authorization));
    }

    @PostMapping()
    public MessageResponse create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody GuestbookCreateRequest request) {
        guestbookService.create(authService.getLoginMember(authorization), request);
        return MessageResponse.createResponse("방명록이 등록되었습니다.");
    }

    @PutMapping("/{id}")
    public MessageResponse update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long id,
            @RequestBody GuestbookCreateRequest request) {
        guestbookService.update(authService.getLoginMember(authorization), id, request);
        return MessageResponse.createResponse("방명록을 수정했습니다.");
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long id) {
        guestbookService.delete(authService.getLoginMember(authorization), id);
        return MessageResponse.createResponse("방명록이 삭제되었습니다.");
    }

    @PostMapping("/{id}/replies")
    public MessageResponse createReply(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long id,
            @RequestBody GuestbookCreateRequest request) {
        guestbookService.createReply(authService.getLoginMember(authorization), id, request);
        return MessageResponse.createResponse("답글이 등록되었습니다.");
    }

    @PutMapping("/replies/{replyId}")
    public MessageResponse updateReply(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long replyId,
            @RequestBody GuestbookCreateRequest request) {
        guestbookService.updateReply(authService.getLoginMember(authorization), replyId, request);
        return MessageResponse.createResponse("답글을 수정했습니다.");
    }

    @DeleteMapping("/replies/{replyId}")
    public MessageResponse deleteReply(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long replyId) {
        guestbookService.deleteReply(authService.getLoginMember(authorization), replyId);
        return MessageResponse.createResponse("답글이 삭제되었습니다.");
    }
}
