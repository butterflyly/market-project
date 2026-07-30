package com.example.demo.Service.Chat;

import com.example.demo.DTO.Chat.ChatRoomCreateResult;
import com.example.demo.DTO.Chat.Response.ChatMessageResponse;
import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Chat.MessageType;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatRoomCommandService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomReadService chatRoomReadService;
    private final ChatMessageRepository chatMessageRepository;
    private final UserReadService userReadService;
    private final ItemReadService itemReadService;
    private final SimpMessagingTemplate messagingTemplate;


    // 채팅방 상태를 넘기기 위해 ChatRoomCreateResult DTO 생성 및 타입으로 설정
    public ChatRoomCreateResult chatRoomCreate(Long itemId, String buyerUsername)
    {
        //  상품 정보를 먼저 조회
        Item item = itemReadService.getItemEntity(itemId);
        Users sellerUser = item.getSeller();
        Users buyerUser = userReadService.getUserEntity(buyerUsername);

        //  본인이 등록한 상품에 채팅을 시도하는지 체크
        if (sellerUser.getUsername().equals(buyerUsername)) {
            throw new BusinessException("본인이 등록한 상품에는 채팅을 할 수 없습니다.");
        }

        //  기존 방 조회
        Optional<ChatRoom> existingRoom = chatRoomReadService.findExistingRoom(item, buyerUser);

        if (existingRoom.isPresent()) {
            return new ChatRoomCreateResult(existingRoom.get().getId(), "기존 채팅방으로 이동합니다.");
        }

        ChatRoom chatRoom = ChatRoom.builder()
                .item(item)
                .buyer(buyerUser)
                .seller(sellerUser)
                .status(ChatRoomStatus.BOTH_IN)
                .build();

        Long newRoomId = chatRoomRepository.save(chatRoom).getId();
        return new ChatRoomCreateResult(newRoomId, "새로운 채팅방이 생성되었습니다.");
    }

    public void leaveRoom(Long roomId, String username) {

        ChatRoom chatRoom = chatRoomReadService.getChatRoomEntity(roomId);
        Users user = userReadService.getUserEntity(username);

        if (chatRoom.isBothLeft()) {
            throw new BusinessException("이미 종료된 채팅방입니다.");
        }

        // 1. 역할 식별 및 상태 변경
        if (chatRoom.getBuyer().getUsername().equals(username)) {
            chatRoom.buyerLeave();
        } else if (chatRoom.getSeller().getUsername().equals(username)) {
            chatRoom.sellerLeave();
        } else {
            throw new BusinessException("해당 채팅방의 참여자가 아닙니다.");
        }

        // 2.  퇴장 메시지 처리 분기
        if (chatRoom.isBothLeft()) {
            // 둘 다 나갔다면 방을 파괴하므로 메시지를 보낼 필요 없이 깔끔하게 청소
            chatMessageRepository.deleteByChatRoomId(roomId);
            chatRoomRepository.delete(chatRoom);
            log.info(" 양쪽 모두 퇴장하여 {}번 방을 삭제했습니다.", roomId);
        } else {
            //  한 명만 나간 경우: 방에 남은 사람에게 퇴장 공지 전송
            String noticeContent = user.getNickname() + "님이 채팅방에서 나갔습니다.";

            // A. DB에 NOTICE 타입으로 안내 메시지 저장
            ChatMessage noticeMessage = ChatMessage.builder()
                    .chatRoom(chatRoom)
                    .sender(user) // 나간 사람을 sender로 지정하거나 null 허용(시스템 공지인 경우)
                    .content(noticeContent)
                    .type(MessageType.NOTICE)
                    .isRead(false)
                    .build();
            chatMessageRepository.save(noticeMessage);

            // B. 웹소켓으로 해당 방 구독자들에게 실시간 브로드캐스팅
            // 프론트엔드가 실시간으로 받고 화면 중앙에 안내 문구
            ChatMessageResponse response = ChatMessageResponse.of(noticeMessage, username);
            messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, response);

            log.info(" {}번 방에 퇴장 공지 전송 완료: {}", roomId, noticeContent);
        }
    }
}
