package com.example.demo.Repository.Review;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Review.Review;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review,Long> {

    //  내가 적은 후기 목록 조회 (작성자가 '나'인 리뷰들)
    Page<Review> findByWriter(Users writer, Pageable pageable);

    //  내가 받은 후기 목록 조회 (수신자가 '나'인 리뷰들 = 판매자로서 받은 평판)
    Page<Review> findByReceiver(Users receiver, Pageable pageable);

    boolean existsByItem(Item item);

    @Modifying
    @Transactional
    @Query("update Review r set r.writer = :ghostUser where r.writer.id in :expiredUserIds")
    void transferWriterToGhost(@Param("expiredUserIds") List<Long> expiredUserIds,
                               @Param("ghostUser") Users ghostUser);

    @Modifying
    @Transactional
    @Query("update Review r set r.receiver = :ghostUser where r.receiver.id in :expiredUserIds")
    void transferReceiverToGhost(@Param("expiredUserIds") List<Long> expiredUserIds,
                                 @Param("ghostUser") Users ghostUser);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.receiver.id = :sellerId")
    Double getAverageRatingBySellerId(@Param("sellerId") Long sellerId);
}
