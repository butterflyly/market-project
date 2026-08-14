package com.example.demo.DTO.Users.Response;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import java.time.format.DateTimeFormatter;

public record MyPageItemResponse(
        Long id,
        String title,
        int price,
        String sellerNickname, // 판매자 이름(닉네임)
        String sellerUsername,
        String categoryName,   // 카테고리명
        String createdAt,
        String updatedAt,
        int views,
        ItemStatus itemStatus,
        String imageUrl,
        boolean hasReviewed    // 마이페이지 전용 리뷰 작성 여부 플래그
) {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Entity -> DTO 변환 정적 메서드
    public static MyPageItemResponse of(Item item) {

        // [핵심 보완] 판매자가 탈퇴했거나 Null일 때 방어
        boolean hasSeller = item.getSeller() != null;

        return new MyPageItemResponse(
                item.getId(),
                item.getTitle(),
                item.getPrice(),
                hasSeller ? item.getSeller().getDisplayNickname() : "탈퇴사용자",
                hasSeller ? item.getSeller().getUsername() : "unknown",
                item.getItemCategory() != null ? item.getItemCategory().getName() : "미지정",
                item.getCreatedAt() != null ? item.getCreatedAt().format(formatter) : "",
                item.getUpdatedAt() != null ? item.getUpdatedAt().format(formatter) : "",
                item.getViews(),
                item.getItemStatus(),
                item.getThumbnailUrl(),
                item.getReview() != null // 리뷰가 연결되어 있으면 true, 없으면 false
        );
    }
}