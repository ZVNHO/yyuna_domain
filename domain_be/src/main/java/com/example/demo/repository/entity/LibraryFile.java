package com.example.demo.repository.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LibraryFile {
    private long id;
    private long memberId;
    private String uploader; // member.name (조회 시 JOIN)
    private String name;
    private long fileSize;
    private byte[] content;  // 파일 내용 (다운로드할 때만 조회)
    private LocalDateTime createdAt;

    public static LibraryFile createFile(long memberId, String name, byte[] content) {
        LibraryFile file = new LibraryFile();
        file.memberId = memberId;
        file.name = name;
        file.fileSize = content.length;
        file.content = content;
        return file;
    }
}
