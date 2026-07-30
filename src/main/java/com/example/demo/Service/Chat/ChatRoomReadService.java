package com.example.demo.Service.Chat;

import com.example.demo.DTO.Chat.Response.ChatBuyerResponse;
import com.example.demo.DTO.Chat.Response.ChatRoomDetailResponse;
import com.example.demo.DTO.Chat.Response.ChatRoomSummaryResponse;
import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Chat.ChatRoom.ChatRoomRepository;
import com.example.demo.Repository.Users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomReadService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageReadService chatMessageReadService;


    // 채팅방 엔티티
    public ChatRoom getChatRoomEntity(Long roomId) {
        return chatRoomRepository.findWithDetailsById(roomId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 채팅방입니다."));
    }


    // 채팅방 존재유무 체크용 메소드
    public Optional<ChatRoom> findExistingRoom(Item item, Users buyerUser) {
        return chatRoomRepository.findByItemAndBuyer(item,buyerUser);
    }

    // 채팅방 조회
    public ChatRoomDetailResponse getChatRoomDetail(Long roomId, String username) {
        // 1. 방 정보 조회 (검증 포함)
        ChatRoom chatRoom = getChatRoomEntity(roomId);

        // 2. 핵심 보완: 참여자 권한 및 '퇴장 여부' 2중 체크
        boolean isBuyer = chatRoom.getBuyer().getUsername().equals(username);
        boolean isSeller = chatRoom.getSeller().getUsername().equals(username);

        // [경우 A] 이 방의 참여자(구매자/판매자)가 아예 아닌 경우
        if (!isBuyer && !isSeller) {
            throw new BusinessException("해당 채팅방에 접근 권한이 없습니다.");
        }

        // [경우 B] 참여자는 맞지만, 본인이 이미 '나가기'를 완료한 방인 경우
        if ((isBuyer && chatRoom.isBuyerLeft()) || (isSeller && chatRoom.isSellerLeft())) {
            throw new BusinessException("이미 퇴장한 채팅방에는 다시 접근할 수 없습니다.");
        }

        // 3. 상대방 유저 객체 확보
        Users partner = chatRoom.getPartner(username);

        return new ChatRoomDetailResponse(
                chatRoom.getId(),
                chatRoom.getItem().getTitle(),
                chatRoom.getItem().getThumbnailUrl(),
                chatRoom.getItem().getId(),
                partner.getNickname(),
                // 메시지 내역 조회
                chatMessageReadService.getMessages(roomId, username)
        );
    }

    // 역할(BUYER/SELLER)에 따른 페이징 조회
    public Page<ChatRoomSummaryResponse> getChatRooms(String username, String role, int page, int size) {
        Users user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ChatRoom> chatRooms;
        if ("BUYER".equals(role)) {
            chatRooms = chatRoomRepository.findAllByBuyerAndStatusIn(
                    user,
                    List.of(ChatRoomStatus.BOTH_IN, ChatRoomStatus.HOST_OUT),
                    pageable
            );
        } else {
            chatRooms = chatRoomRepository.findAllBySellerAndStatusIn(
                    user,
                    List.of(ChatRoomStatus.BOTH_IN, ChatRoomStatus.GUEST_OUT),
                    pageable
            );
        }

        return chatRooms.map(room -> convertToSummary(room, username));
    }

    // 엔티티 -> DTO 변환 공통 로직
    private ChatRoomSummaryResponse convertToSummary(ChatRoom room, String loginUsername) {
        // 내가 구매자면 상대는 판매자, 아니면 구매자가 상대방
        Users partner = room.getBuyer().getUsername().equals(loginUsername)
                ? room.getSeller()
                : room.getBuyer();

        return new ChatRoomSummaryResponse(
                room.getId(),
                room.getItem().getTitle(),
                partner.getNickname()
        );
    }


    // 채팅방 구매자 리스트 조회
    public List<ChatBuyerResponse> getBuyersByItemId(Long itemId, String sellerUsername) {

        // 1. 해당 상품(itemId)으로 개설된 모든 채팅방 조회
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByItemId(itemId);

        // 2. 채팅방 참여자 중 '구매자'들을 수집하되, 중복 유저는 Set으로 제거
        return chatRooms.stream()
                .map(ChatRoom::getBuyer) // 채팅방의 구매자 엔티티 추출
                .filter(Objects::nonNull) // null 방어
                .filter(buyer -> !buyer.getUsername().equals(sellerUsername)) // 혹시 모를 판매자 본인 필터링
                .distinct() // 중복 구매자 제거
                .map(ChatBuyerResponse::from) // DTO 변환
                .toList();
    }
}
