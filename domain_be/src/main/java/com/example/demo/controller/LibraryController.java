package com.example.demo.controller;

import com.example.demo.controller.dto.GuestbookCreateRequest;
import com.example.demo.controller.dto.LibraryFileResponse;
import com.example.demo.controller.dto.MessageResponse;
import com.example.demo.repository.entity.LibraryFile;
import com.example.demo.service.AuthService;
import com.example.demo.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

//자료실 (로그인한 회원만 사용)
@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryController {
    private final AuthService authService;
    private final LibraryService libraryService;

    @GetMapping()
    public List<LibraryFileResponse> list(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return libraryService.getAll(authService.getLoginMember(authorization));
    }

    @PostMapping()
    public MessageResponse upload(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("file") MultipartFile file) {
        libraryService.upload(authService.getLoginMember(authorization), file);
        return MessageResponse.createResponse("파일이 등록되었습니다.");
    }

    //올린 파일이 브라우저에서 바로 실행되지 않도록 항상 첨부파일로 내려준다
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long id) {
        authService.getLoginMember(authorization);
        LibraryFile file = libraryService.download(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(file.getContent());
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long id) {
        libraryService.delete(authService.getLoginMember(authorization), id);
        return MessageResponse.createResponse("파일이 삭제되었습니다.");
    }

    @PostMapping("/{id}/replies")
    public MessageResponse createReply(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long id,
            @RequestBody GuestbookCreateRequest request) {
        libraryService.createReply(authService.getLoginMember(authorization), id, request);
        return MessageResponse.createResponse("답글이 등록되었습니다.");
    }

    @DeleteMapping("/replies/{replyId}")
    public MessageResponse deleteReply(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable long replyId) {
        libraryService.deleteReply(authService.getLoginMember(authorization), replyId);
        return MessageResponse.createResponse("답글이 삭제되었습니다.");
    }
}
