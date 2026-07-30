package com.example.demo.DTO.Chat.Request;

import com.example.demo.Entity.Chat.MessageType;

public record ChatMessageRequest(
        Long roomId,
        String content,
        MessageType type
) {
}
