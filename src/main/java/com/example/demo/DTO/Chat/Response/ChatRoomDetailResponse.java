package com.example.demo.DTO.Chat.Response;

import java.util.List;

public record ChatRoomDetailResponse(
        Long roomId,
        String itemTitle,
        String itemImageUrl,
        Long itemId,
        String partnerNickname ,
        List<ChatMessageResponse> messages
) {
}
