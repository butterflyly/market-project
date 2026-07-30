package com.example.demo.Repository.Item.Item;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item,Long> , ItemRepositoryCustom {

    //  deleted 가 true 이면서, updatedAt(삭제일시)이 한 달 전인 상품 ID 추출
    @Query("select i.id from Item i where i.deleted = true and i.updatedAt < :beforeTime")
    List<Long> findIdsByDeletedTrueAndUpdatedAtBefore(@Param("beforeTime") LocalDateTime beforeTime);

    Optional<Item> findByIdAndDeletedFalse(Long itemId);

    Page<Item> findAllBySellerAndDeletedFalse(Users user, Pageable pageable);

    long countByDeletedTrue();

    /*
    // ➔  변경 후 (엔티티 필드명이 itemStatus일 때)
    List<Item> findBySellerAndItemStatus(Users seller, ItemStatus status);
    */

    @Modifying
    @Transactional
    @Query("update Item i set i.seller = :ghostUser where i.seller.id in :expiredUserIds")
    void transferSellerToGhost(@Param("expiredUserIds") List<Long> expiredUserIds,
                               @Param("ghostUser") Users ghostUser);


}
