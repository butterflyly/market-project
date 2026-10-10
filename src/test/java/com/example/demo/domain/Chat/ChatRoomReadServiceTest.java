package com.example.demo.domain.Chat;

import com.example.demo.DTO.Chat.Response.ChatRoomDetailResponse;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Chat.ChatMessageReadService;
import com.example.demo.Service.Chat.ChatRoomReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class ChatRoomReadServiceTest {

    @InjectMocks
    private ChatRoomReadService chatRoomReadService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageReadService chatMessageReadService;


    @Test
    void 채팅방_상세조회_성공() {
        // given
        Long roomId = 1L;
        String username = "buyerUser";

        Users buyer = createMockUsers(username);
        Users seller = createMockUsers("sellerUser");
        Item item = createMockItem(1L, seller);

        ChatRoom chatRoom = ChatRoom.builder()
                .id(roomId)
                .item(item)
                .buyer(buyer)
                .seller(seller)
                .build();

        given(chatRoomRepository.findWithDetailsById(roomId)).willReturn(Optional.of(chatRoom));
        given(chatMessageReadService.getMessages(roomId, username)).willReturn(List.of());

        // when
        ChatRoomDetailResponse response = chatRoomReadService.getChatRoomDetail(roomId, username);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isEqualTo(roomId);
    }

    @Test
    void 채팅방_상세조회_실패_권한없음() {
        // given
        Long roomId = 1L;
        String unauthorizedUser = "strangerUser"; // 방에 속하지 않은 유저

        Users buyer = createMockUsers("buyerUser");
        Users seller = createMockUsers("sellerUser");
        Item item = createMockItem(1L, seller);

        ChatRoom chatRoom = ChatRoom.builder()
                .id(roomId)
                .item(item)
                .buyer(buyer)
                .seller(seller)
                .build();

        given(chatRoomRepository.findWithDetailsById(roomId)).willReturn(Optional.of(chatRoom));

        // when & then
        assertThatThrownBy(() -> chatRoomReadService.getChatRoomDetail(roomId, unauthorizedUser))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 채팅방에 접근 권한이 없습니다.");
    }
}
