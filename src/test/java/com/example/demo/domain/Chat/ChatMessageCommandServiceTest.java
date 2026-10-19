package com.example.demo.domain.Chat;

import com.example.demo.DTO.Alram.ChatNotificationEvent;
import com.example.demo.DTO.Chat.Request.ChatMessageRequest;
import com.example.demo.DTO.Chat.Response.ChatMessageResponse;
import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Chat.MessageType;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Chat.ChatMessageCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ChatMessageCommandServiceTest {

    @InjectMocks
    private ChatMessageCommandService chatMessageCommandService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void 채팅메세지_생성_성공()
    {
        // given
        ChatMessageRequest chatMessageRequest = new ChatMessageRequest(1L,"테스트 내용", MessageType.TALK);

        Users seller = createMockUsers("판매유저");
        Users buyerUsers  = createMockUsers("구매유저");
        Item item = createMockItem(1L,seller);

        ChatRoom chatRoom = ChatRoom.builder()
                .id(1L)
                .item(item)
                .buyer(buyerUsers)
                .seller(seller)
                .status(ChatRoomStatus.BOTH_IN)
                .build();

        given(chatRoomRepository.findById(chatMessageRequest.roomId())).willReturn(Optional.of(chatRoom));
        given(userRepository.findByUsernameAndDeletedFalse("판매유저")).willReturn(Optional.of(seller));

        given(chatMessageRepository.save(any(ChatMessage.class))).
                willAnswer(invocation -> invocation.getArgument(0));

        // when
        ChatMessageResponse response = chatMessageCommandService.saveMessage(chatMessageRequest, "판매유저");

        // then
        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("테스트 내용");

        // 이벤트 퍼블리셔가 정상적으로 호출되었는지 검증 (필요시)
        verify(eventPublisher, times(1)).publishEvent(any(ChatNotificationEvent.class));
    }

}
