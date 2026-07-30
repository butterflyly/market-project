package com.example.demo.DTO.Alram;

public record ChatNotificationEvent(
        Long receiverId,
        String senderNickname,
        String roomUrl
) {}