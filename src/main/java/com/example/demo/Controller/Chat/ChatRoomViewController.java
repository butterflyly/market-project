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

    @GetMapping("/{roomId}")
    public String chatRoomDetail(@PathVariable Long roomId, Model model) {
        // 리액트가 읽을 roomId 정도만 전달하거나, URL에서 리액트가 직접 추출하도록 함
        model.addAttribute("roomId", roomId);
        return "chat/chat-room"; // react-chat-root만 있는 빈 타임리프 파일
    }
}
