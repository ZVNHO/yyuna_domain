package com.example.demo.service;

import com.example.demo.controller.dto.GuestbookCreateRequest;
import com.example.demo.controller.dto.LibraryFileResponse;
import com.example.demo.controller.dto.LibraryReplyResponse;
import com.example.demo.repository.LibraryRepository;
import com.example.demo.repository.entity.LibraryFile;
import com.example.demo.repository.entity.LibraryReply;
import com.example.demo.repository.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibraryService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_REPLY_LENGTH = 500;

    private final LibraryRepository libraryRepository;

    public List<LibraryFileResponse> getAll(Member loginMember) {
        Map<Long, List<LibraryReplyResponse>> replies = libraryRepository.findAllReplies().stream()
                .map(entity -> LibraryReplyResponse.responseReply(entity, loginMember.getId()))
                .collect(Collectors.groupingBy(LibraryReplyResponse::getFileId));

        return libraryRepository.findAll().stream()
                .map(entity -> LibraryFileResponse.responseFile(
                        entity, loginMember.getId(), replies.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    @Transactional
    public void upload(Member loginMember, MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일은 등록할 수 없습니다.");
        }
        //브라우저가 보낸 경로는 버리고 파일 이름만 저장
        String name = StringUtils.getFilename(StringUtils.cleanPath(String.valueOf(file.getOriginalFilename())));
        if (!StringUtils.hasText(name) || name.length() > MAX_NAME_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일 이름이 올바르지 않습니다.");
        }
        try {
            libraryRepository.insertFile(LibraryFile.createFile(loginMember.getId(), name, file.getBytes()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 읽지 못했습니다.", e);
        }
    }

    public LibraryFile download(long id) {
        LibraryFile file = libraryRepository.findWithContentById(id);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다.");
        }
        return file;
    }

    @Transactional
    public void delete(Member loginMember, long id) {
        LibraryFile file = libraryRepository.findById(id);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다.");
        }
        if (file.getMemberId() != loginMember.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 올린 파일만 삭제할 수 있습니다.");
        }
        libraryRepository.deleteById(id);
    }

    @Transactional
    public void createReply(Member loginMember, long fileId, GuestbookCreateRequest request) {
        String content = request.getContent();
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용을 입력해주세요.");
        }
        if (content.length() > MAX_REPLY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내용은 500자까지 입력할 수 있습니다.");
        }
        if (libraryRepository.findById(fileId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 없습니다.");
        }
        libraryRepository.insertReply(LibraryReply.createReply(fileId, loginMember.getId(), content));
    }

    @Transactional
    public void deleteReply(Member loginMember, long replyId) {
        LibraryReply reply = libraryRepository.findReplyById(replyId);
        if (reply == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "답글이 없습니다.");
        }
        if (reply.getMemberId() != loginMember.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 쓴 답글만 삭제할 수 있습니다.");
        }
        libraryRepository.deleteReplyById(replyId);
    }
}
