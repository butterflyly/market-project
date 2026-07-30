package com.example.demo.Controller.Chat;

import com.example.demo.DTO.Chat.Response.ChatRoomDetailResponse;
import com.example.demo.Service.Chat.ChatMessageCommandService;
import com.example.demo.Service.Chat.ChatRoomReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/chat/room")
@RequiredArgsConstructor
public class ChatRoomViewController {

    private final ChatRoomReadService chatRoomReadService;
    private final ChatMessageCommandService chatMessageCommandService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/{roomId}")
    public String chatRoomDetail(@PathVariable Long roomId, Model model, @AuthenticationPrincipal UserDetails userDetails)
    {
        ChatRoomDetailResponse chatRoom = chatRoomReadService.getChatRoomDetail(roomId, userDetails.getUsername());

        // 1. 채팅방 들어왔으니 읽음처리
        chatMessageCommandService.markMessagesAsRead(roomId, userDetails.getUsername());

        // 2. 이미 들어와 있는 상대방 화면의 '1'도 실시간 삭제되도록 웹소켓 알림 발송
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, Map.of("type", "READ"));

        model.addAttribute("roomInfo", chatRoom);

        return "chat/chat-room";
    }
}
