package com.example.demo.Repository.Chat.ChatRoom;

import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ChatRoomRepositoryCustom {

    // 탈퇴하는 유저가 '아직 나가지 않은' 모든 채팅방 조회
    List<ChatRoom> findAllActiveRoomsByUserId(Long id);

    void transferBuyerToGhost(List<Long> userIds,  Users ghostUser);

    void transferSellerToGhost( List<Long> userIds,  Users ghostUser);

    void deletePureGhostRooms(Long ghostUserId);

    //  구매자 전용: 특정 상태 목록(In)에 포함되는 방만 페이징 조회
    Page<ChatRoom> findAllByBuyerAndStatusIn(Users buyer, List<ChatRoomStatus> statuses, Pageable pageable);

    //  판매자 전용: 특정 상태 목록(In)에 포함되는 방만 페이징 조회
    Page<ChatRoom> findAllBySellerAndStatusIn(Users seller, List<ChatRoomStatus> statuses, Pageable pageable);
}

