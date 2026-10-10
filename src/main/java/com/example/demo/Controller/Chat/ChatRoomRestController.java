package com.example.demo.Controller.Chat;

import com.example.demo.DTO.Chat.Response.ChatRoomDetailResponse;
import com.example.demo.Service.Chat.ChatMessageCommandService;
import com.example.demo.Service.Chat.ChatRoomReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat-room")
@RequiredArgsConstructor
public class ChatRoomRestController {

    private final ChatRoomReadService chatRoomReadService;
    private final ChatMessageCommandService chatMessageCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{roomId}")
    public ResponseEntity<ChatRoomDetailResponse> getChatRoomDetail(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {

        ChatRoomDetailResponse chatRoom = chatRoomReadService.getChatRoomDetail(roomId, userDetails.getUsername());

        // 1. 읽음 처리
        chatMessageCommandService.markMessagesAsRead(roomId, userDetails.getUsername());

        // 2. 상대방 화면 unread count 실시간 갱신 알림
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, Map.of("type", "READ"));

        return ResponseEntity.ok(chatRoom);
    }
}