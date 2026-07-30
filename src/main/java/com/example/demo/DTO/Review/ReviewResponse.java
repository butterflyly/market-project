package com.example.demo.DTO.Review;

import com.example.demo.Entity.Review.Review;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long itemId,
        String itemTitle,
        Long writerId,
        String writerNickname,
        String writerProfileUrl,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Review review) {

        // 엔티티 도메인 메서드로 깔끔하게 상태 판별
        boolean hasItem = review.hasActiveItem();
        boolean hasWriter = review.hasActiveWriter();

        return new ReviewResponse(
                review.getId(),
                hasItem ? review.getItem().getId() : null,
                hasItem ? review.getItem().getTitle() : "삭제된 상품의 리뷰입니다.",

                hasWriter ? review.getWriter().getId() : null,
                hasWriter ? review.getWriter().getDisplayNickname() : "탈퇴사용자",
                hasWriter ? review.getWriter().getProfileUrl() : "/images/default-profile.png",

                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}