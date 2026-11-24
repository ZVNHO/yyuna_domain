package com.example.demo.controller.dto;

import com.example.demo.repository.entity.LibraryFile;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class LibraryFileResponse {
    private long id;
    private String uploader;
    private String name;
    private long fileSize;
    private LocalDateTime createdAt;
    private boolean mine; // 로그인한 회원이 올린 파일인지 (삭제 버튼 표시용)
    private List<LibraryReplyResponse> replies;

    public static LibraryFileResponse responseFile(LibraryFile entity, long loginMemberId, List<LibraryReplyResponse> replies) {
        LibraryFileResponse response = new LibraryFileResponse();
        response.id = entity.getId();
        response.uploader = entity.getUploader();
        response.name = entity.getName();
        response.fileSize = entity.getFileSize();
        response.createdAt = entity.getCreatedAt();
        response.mine = entity.getMemberId() == loginMemberId;
        response.replies = replies;
        return response;
    }
}
