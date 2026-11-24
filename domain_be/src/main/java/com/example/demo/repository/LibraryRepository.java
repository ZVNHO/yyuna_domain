package com.example.demo.repository;

import com.example.demo.repository.entity.LibraryFile;
import com.example.demo.repository.entity.LibraryReply;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface LibraryRepository {

    int insertFile(LibraryFile file);

    List<LibraryFile> findAll();

    LibraryFile findById(long id);

    LibraryFile findWithContentById(long id);

    void deleteById(long id);

    int insertReply(LibraryReply reply);

    List<LibraryReply> findAllReplies();

    LibraryReply findReplyById(long id);

    void deleteReplyById(long id);
}
