package com.example.demo.repository;

import com.example.demo.repository.entity.Guestbook;
import com.example.demo.repository.entity.GuestbookReply;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface GuestbookRepository {

    int insertGuestbook(Guestbook guestbook);

    List<Guestbook> findAll();

    Guestbook findById(long id);

    int updateGuestbook(Guestbook guestbook); // 내용 수정 (updated_at 기록)

    void deleteById(long id);

    int insertReply(GuestbookReply reply);

    List<GuestbookReply> findAllReplies();

    GuestbookReply findReplyById(long id);

    int updateReply(GuestbookReply reply); // 내용 수정 (updated_at 기록)

    void deleteReplyById(long id);
}
