package com.example.demo.domain.Chat;

import com.example.demo.DTO.Chat.ChatRoomCreateResult;
import com.example.demo.DTO.Chat.Request.ChatMessageRequest;
import com.example.demo.DTO.Chat.Response.ChatMessageResponse;
import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Entity.Chat.MessageType;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Chat.ChatMessageCommandService;
import com.example.demo.Service.Chat.ChatRoomCommandService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;


import java.util.List;
import static org.mockito.ArgumentMatchers.eq;
import static com.example.demo.TestFixture.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;

import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
public class ChatServiceIntegrationTest {

    @Autowired
    private ChatRoomCommandService chatRoomCommandService;

    @Autowired
    private ChatMessageCommandService chatMessageCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;



    @Test
    void 한명만_채팅방_나가기_성공_공지메시지발송() {
        // given
        Users seller = createMockUsersTotal("seller");
        userRepository.save(seller);

        Users buyer = createMockUsersTotal2("buyer");
        userRepository.save(buyer);

        Item item = createMockItem(null, seller);
        itemRepository.save(item);

        // 채팅방 생성
        ChatRoomCreateResult result = chatRoomCommandService.chatRoomCreate(item.getId(), "buyer");
        Long roomId = result.roomId();

        // when (구매자가 방을 나감)
        chatRoomCommandService.leaveRoom(roomId, "buyer");

        // then
        // 1. 방 상태가 한 명만 나간 상태(HOST_OUT 등)로 변경되었는지,
        // 2. NOTICE 타입의 메시지가 DB에 저장되었는지 검증
        List<ChatMessage> messages =
                chatMessageRepository.findByChatRoomId(roomId, PageRequest.of(0, 10)).getContent();
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getType()).isEqualTo(MessageType.NOTICE);
        assertThat(messages.get(0).getContent()).contains("테스트유저2님이 채팅방에서 나갔습니다.");

    }

    @Test
    void 메세지전송_성공()
    {
        // given
        Users seller = createMockUsersTotal("seller");
        userRepository.save(seller);

        Users buyer = createMockUsersTotal2("buyer");
        userRepository.save(buyer);

        Item item = createMockItem(null, seller);
        itemRepository.save(item);

        // 채팅방 생성 (미리 방이 존재한다는 가정)
        ChatRoomCreateResult roomResult = chatRoomCommandService.chatRoomCreate(item.getId(), "buyer");
        Long roomId = roomResult.roomId();

        ChatMessageRequest request = new ChatMessageRequest(roomId, "안녕하세요 문의드립니다!", MessageType.TALK);

        // when (구매자가 메시지 전송)
        ChatMessageResponse response = chatMessageCommandService.saveMessage(request, "buyer");

        // then
        // 1. 메시지가 DB에 정상적으로 저장되었는지 검증
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, PageRequest.of(0, 10)).getContent();
        assertThat(messages).isNotEmpty();
        assertThat(messages.get(0).getContent()).isEqualTo("안녕하세요 문의드립니다!");
        assertThat(messages.get(0).getType()).isEqualTo(MessageType.TALK);

    }
}