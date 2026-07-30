package com.example.demo.Repository.Chat.ChatMessage;

import com.example.demo.Entity.Chat.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom{

    Slice<ChatMessage> findByChatRoomId(Long roomId, Pageable pageable);

    void deleteByChatRoomId(Long roomId);
}
