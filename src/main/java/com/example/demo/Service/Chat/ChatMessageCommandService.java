package com.example.demo.Service.Chat;

import com.example.demo.DTO.Alram.ChatNotificationEvent;
import com.example.demo.DTO.Chat.Request.ChatMessageRequest;
import com.example.demo.DTO.Chat.Response.ChatMessageResponse;
import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatMessageCommandService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher; // 스프링 내장 확성기


    public ChatMessageResponse saveMessage(ChatMessageRequest chatMessageRequest, String username) {
        ChatRoom room = chatRoomRepository.findById(chatMessageRequest.roomId())
                .orElseThrow(() -> new BusinessException("존재하지 않는 채팅방입니다."));
        Users sender = userRepository.findByUsernameAndDeletedFalse(username).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(chatMessageRequest.content())
                .type(chatMessageRequest.type()) // TALK 혹은 IMAGE 저장
                .isRead(false)
                .build();

        chatMessageRepository.save(message);

        // 현재 로그인한 유저(보낸이)의 '상대방' 엔티티를 자동으로 찾음
        Users receiver = room.getPartner(username);

        String targetUrl = "/chat/room/" + room.getId();

        eventPublisher.publishEvent(new ChatNotificationEvent(receiver.getId(), sender.getNickname(), targetUrl));

        return ChatMessageResponse.of(message, username);
    }

    // 읽음 로직
    public void markMessagesAsRead(Long roomId, String loginUsername) {
        // 이 방의 메시지 중, 내가 보낸 것이 아니고(상대방이 보낸 것), 아직 안 읽은 것들만 업데이트
        chatMessageRepository.markAsReadWithQuerydsl(roomId, loginUsername);
    }
}
