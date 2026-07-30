package com.example.demo.DTO.Chat.Response;

import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Entity.Chat.MessageType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public record ChatMessageResponse(

        String senderNickname,
        String senderUsername,
        String content,
        LocalDateTime createdAt,
        boolean isRead,
        boolean isMine,    // 로그인한 유저와 sender 를 비교해서 서비스에서 넣어줌
        String type        // "TALK", "IMAGE", "NOTICE" 등
) {

    public static ChatMessageResponse of(ChatMessage m, String loginUsername) {
        boolean isMineResult = false;
        if (m.getSender() != null) {
            isMineResult = m.getSender().getUsername().equals(loginUsername);
        }

        assert m.getSender() != null;

        //  null 방어: senderNickname이 없으면 Users 엔티티에서 직접 가져옴
        String nickname = m.getSenderNickname();
        if (nickname == null && m.getSender() != null) {
            nickname = m.getSender().getNickname();
        }

        return new ChatMessageResponse(
                nickname,
                m.getSender().getUsername(),
                m.getContent(),
                m.getCreatedAt() != null ? m.getCreatedAt() : LocalDateTime.now(),
                m.isRead(),
                isMineResult,
                m.getType() != null ? m.getType().name() : "TALK"
        );
    }
}
