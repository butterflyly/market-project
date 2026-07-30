package com.example.demo.Controller.Chat;

import com.example.demo.DTO.Chat.Request.ChatMessageRequest;
import com.example.demo.DTO.Chat.Response.ChatMessageResponse;
import com.example.demo.Service.Chat.ChatMessageCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatMessageCommandService chatMessageCommandService;
    private final SimpMessagingTemplate messagingTemplate; // 메시지 전송 도구

    @MessageMapping("/chat/message")
    public void message(ChatMessageRequest request, Principal principal) {

        if (principal == null) {
            log.error(" 웹소켓 인증 실패: 유저 정보가 없습니다.");
            return;
        }

        // principal.getName()을 통해 로그인 유저의 username(아이디)을 안전하게 추출
        String username = principal.getName();
        log.info("웹소켓 통신 유저 아이디: {}", username);

        // 서비스 로직 호출 시 추출한 username 전달
        ChatMessageResponse savedMessage = chatMessageCommandService.saveMessage(request, username);

        // 구독자들에게 메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/chat/room/" + request.roomId(), savedMessage);
    }


    /**
     *  [추가] 실시간 읽음(READ) 이벤트 처리
     * JS에서 stompClient.publish({
     * destination: "/pub/chat/read", body: JSON.stringify({ roomId: roomId }) }) 호출 시 수신
     */
    @MessageMapping("/chat/read")
    public void readMessage(Map<String, Object> payload, Principal principal) {
        if (principal == null) return;

        // roomId 추출 (JS에서 넘겨준 값)
        Long roomId = Long.parseLong(payload.get("roomId").toString());
        String username = principal.getName();

        log.info(" [STOMP READ 요청] roomId: {}, username: {}", roomId, username);

        // 1. DB 읽음 업데이트
        chatMessageCommandService.markMessagesAsRead(roomId, username);

        // 2. 방 구독자 전체에게 "이 방 메시지 다 읽음(type: READ)" 알림 전파
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, Map.of("type", "READ"));
    }
}
