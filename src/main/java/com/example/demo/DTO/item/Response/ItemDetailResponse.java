package com.example.demo.DTO.item.Response;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public record ItemDetailResponse(
        Long id,
        String title,
        String content,
        int price,
        Long sellerId,
        String sellerNickname,
        String sellerDisplayName,
        String sellerUsername,
        String sellerProfileUrl,
        String categoryName,
        Long categoryId,
        String createdAt,
        String updatedAt,
        String finaldestructionDate,
        int views,
        int likeCount,
        boolean isOwner,
        boolean isLiked,
        List<ImageInfo> itemImageList,
        ItemStatus itemStatus
) {
    // 이미지 ID와 URL을 쌍으로 묶어줄 inner record
    public record ImageInfo(Long id, String url) {}

        private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");


        public static ItemDetailResponse of(Item item, boolean isOwner, boolean isLiked) {
            String deletedAt = "-";
            if (item.getUpdatedAt() != null) {
                deletedAt = item.getUpdatedAt().plusMonths(1).format(formatter);
            }

            List<ImageInfo> images = item.getItemImageList().stream()
                    .map(img -> new ImageInfo(img.getImageId(), img.getUrl()))
                    .toList();

            // 정상 판매자 존재 여부 체크
            boolean hasSeller = item.hasActiveSeller();

            return new ItemDetailResponse(
                    item.getId(),
                    item.getTitle(),
                    item.getContent(),
                    item.getPrice(),
                    hasSeller ? item.getSeller().getId() : null,
                    hasSeller ? item.getSeller().getNickname() : "탈퇴사용자",
                    hasSeller ? item.getSeller().getDisplayNickname() : "탈퇴사용자",
                    hasSeller ? item.getSeller().getUsername() : "unknown",
                    hasSeller ? item.getSeller().getProfileUrl() : "/images/default-profile.png",
                    item.getItemCategory().getName(),
                    item.getItemCategory().getId(),
                    item.getCreatedAt() != null ? item.getCreatedAt().format(formatter) : "-",
                    item.getUpdatedAt() != null ? item.getUpdatedAt().format(formatter) : "-",
                    deletedAt,
                    item.getViews(),
                    item.getLikeCount(),
                    isOwner,
                    isLiked,
                    images,
                    item.getItemStatus()
            );
        }
}