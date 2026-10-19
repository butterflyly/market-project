package com.example.demo.domain.Chat;

import com.example.demo.DTO.Chat.ChatRoomCreateResult;
import com.example.demo.DTO.item.Request.ItemCreateRequest;
import com.example.demo.Entity.Chat.ChatMessage;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Chat.MessageType;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatMessage.ChatMessageRepository;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Service.Chat.ChatRoomCommandService;
import com.example.demo.Service.Chat.ChatRoomReadService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ChatRoomCommandServiceTest {

    @InjectMocks
    private ChatRoomCommandService chatRoomCommandService;

    @Mock
    private ItemReadService itemReadService;

    @Mock
    private UserReadService userReadService;

    @Mock
    private ChatRoomReadService chatRoomReadService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;


    @Test
    void 채팅방_생성_성공()
    {
        // given
        // 1. 필요 변수 및 객체 생성
        String username = "정상유저";
        String buyername = "구매유저";
        Long itemId = 1L;
        Users users = createMockUsers(username); // 경고 0회 유저
        Users buyerUsers = createMockUsers(buyername);
        ItemCreateRequest request = new ItemCreateRequest("제목", 10000,
                "내용", 1L, null);
        ItemCategory category = ItemCategory.builder().id(1L).name("디지털기기").build();
        Item item = createMockItem(itemId,users);
        Long generatedRoomId = 100L;

        // repository.save()가 호출될 때 ID가 부여된 ChatRoom을 리턴하도록 스터빙
        ChatRoom savedChatRoom = ChatRoom.builder()
                .id(generatedRoomId)
                .item(item)
                .buyer(buyerUsers)
                .seller(users)
                .status(ChatRoomStatus.BOTH_IN)
                .build();

        given(itemReadService.getItemEntity(itemId)).willReturn(item);
        given(userReadService.getUserEntity(buyername)).willReturn(buyerUsers);
        given(chatRoomReadService.findExistingRoom(item, buyerUsers)).willReturn(Optional.empty());
        given(chatRoomRepository.save(any())).willReturn(savedChatRoom);

        // when
        ChatRoomCreateResult chatRoomCreateResult =  chatRoomCommandService.chatRoomCreate(itemId,buyername);

        // then
        assertThat(chatRoomCreateResult.roomId()).isEqualTo(generatedRoomId);
        assertThat(chatRoomCreateResult.message()).isEqualTo("새로운 채팅방이 생성되었습니다.");

        verify(itemReadService , times(1)).getItemEntity(itemId);
        verify(userReadService , times(1)).getUserEntity(buyername);
        verify(chatRoomReadService , times(1)).findExistingRoom(item, buyerUsers);
    }

    @Test
    void 채팅방_생성_실패_본인이_구매자()
    {
        String username = "정상유저";
        Long itemId = 1L;
        Users users = createMockUsers(username); // 경고 0회 유저
        Users buyerUsers = createMockUsers(username);
        ItemCreateRequest request = new ItemCreateRequest("제목", 10000,
                "내용", 1L, null);
        ItemCategory category = ItemCategory.builder().id(1L).name("디지털기기").build();
        Item item = createMockItem(itemId,users);

        given(itemReadService.getItemEntity(itemId)).willReturn(item);
        given(userReadService.getUserEntity(username)).willReturn(buyerUsers);

        // when & then
        assertThatThrownBy(() -> chatRoomCommandService.chatRoomCreate(itemId,username)).
                isInstanceOf(BusinessException.class)
                .hasMessage("본인이 등록한 상품에는 채팅을 할 수 없습니다.");
    }

    @Test
    void 채팅방_생성_실패_이미있음()
    {
        // given
        // 1. 필요 변수 및 객체 생성
        String username = "정상유저";
        String buyername = "구매유저";
        Long itemId = 1L;
        Users users = createMockUsers(username); // 경고 0회 유저
        Users buyerUsers = createMockUsers(buyername);
        Item item = createMockItem(itemId,users);
        Long generatedRoomId = 100L;

        ChatRoom savedChatRoom = ChatRoom.builder()
                .id(generatedRoomId)
                .item(item)
                .buyer(buyerUsers)
                .seller(users)
                .status(ChatRoomStatus.BOTH_IN)
                .build();

        given(itemReadService.getItemEntity(itemId)).willReturn(item);
        given(userReadService.getUserEntity(buyername)).willReturn(buyerUsers);
        given(chatRoomReadService.findExistingRoom(item, buyerUsers)).willReturn(Optional.ofNullable(savedChatRoom));

        // when
        ChatRoomCreateResult chatRoomCreateResult =  chatRoomCommandService.chatRoomCreate(itemId,buyername);

        // then
        assertThat(chatRoomCreateResult.roomId()).isEqualTo(generatedRoomId);
        assertThat(chatRoomCreateResult.message()).isEqualTo("기존 채팅방으로 이동합니다.");

        verify(itemReadService , times(1)).getItemEntity(itemId);
        verify(userReadService , times(1)).getUserEntity(buyername);
        verify(chatRoomReadService , times(1)).findExistingRoom(item, buyerUsers);
    }

    @Test
    void 채팅방_퇴장_성공()
    {
        // given
        // 1. 필요 변수 및 객체 생성
        String username = "정상유저";
        String buyername = "구매유저";
        Long itemId = 1L;
        Users users = createMockUsers(username); // 경고 0회 유저
        Users buyerUsers = createMockUsers(buyername);
        Item item = createMockItem(itemId,users);
        Long generatedRoomId = 100L;

        ChatRoom savedChatRoom = ChatRoom.builder()
                .id(generatedRoomId)
                .item(item)
                .buyer(buyerUsers)
                .seller(users)
                .status(ChatRoomStatus.BOTH_IN)
                .build();

        ChatMessage noticeMessage = ChatMessage.builder()
                .chatRoom(savedChatRoom)
                .sender(users) // 나간 사람을 sender로 지정하거나 null 허용(시스템 공지인 경우)
                .content(users.getNickname() + "님이 채팅방에서 나갔습니다.")
                .type(MessageType.NOTICE)
                .isRead(false)
                .build();


        given(chatRoomReadService.getChatRoomEntity(generatedRoomId)).willReturn(savedChatRoom);
        given(userReadService.getUserEntity(username)).willReturn(users);
        given( chatMessageRepository.save(any())).willReturn(noticeMessage);

        // 명시적으로 아무것도 하지 않도록(doNothing) 스터빙
        willDoNothing().given(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        // when & then
        assertThatCode(() -> chatRoomCommandService.leaveRoom(generatedRoomId,username))
                .doesNotThrowAnyException();

    }

}
