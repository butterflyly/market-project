package com.example.demo.Repository.Chat.ChatRoom;

import com.example.demo.Entity.Chat.ChatRoom;
import com.example.demo.Entity.Chat.ChatRoomStatus;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom , Long>  , ChatRoomRepositoryCustom{

    Optional<ChatRoom> findByItemAndBuyer(Item item, Users buyerUser);

    List<ChatRoom> findAllByItemId(Long itemId);

    // ChatRoom 조회 시 Item과 Item의 Images까지 한 번에 패치 조인
    @EntityGraph(attributePaths = {"item", "item.itemImageList", "buyer", "seller"})
    Optional<ChatRoom> findWithDetailsById(Long roomId);
}
