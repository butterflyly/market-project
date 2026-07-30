package com.example.demo.DTO.Chat.Response;

public record ChatRoomSummaryResponse(
        Long roomId,
        String itemTitle,
        String partnerNickname // 대화 상대방의 이름
) {
}