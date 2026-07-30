package com.example.demo.Service.Review;

import com.example.demo.DTO.Review.ReviewRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Review.Review;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Review.ReviewRepository;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final ItemReadService itemReadService;
    private final ReviewReadService reviewReadService;
    private final UserReadService userReadService;


    public void createReview(Long itemId, ReviewRequest dto, String loginUsername) {
        Item item = itemReadService.getItemEntity(itemId);

        // 1. [안전장치] 상품이 '판매 완료(SOLD_OUT)' 상태인지 확인
        // (ItemStatus Enum을 사용 중이라고 가정)
        if (item.getItemStatus() != ItemStatus.SOLD_OUT) {
            throw new BusinessException("거래가 완료된 상품에 대해서만 후기를 작성할 수 있습니다.");
        }

        // 2. [안전장치] 진짜 구매자가 맞는지 확인 (ghost/null 체크 포함)
        if (item.getBuyer() == null || !item.getBuyer().getUsername().equals(loginUsername)) {
            throw new BusinessException("실제 구매자만 후기를 남길 수 있습니다.");
        }

        // 3. [안전장치] 자가 리뷰 방지 (본인이 본인에게 리뷰 쓰는 상황 예방)
        if (item.getSeller().getUsername().equals(loginUsername)) {
            throw new BusinessException("본인이 판매한 상품에는 후기를 남길 수 없습니다.");
        }

        // 4. [안전장치] 중복 작성 방지
        if (reviewRepository.existsByItem(item)) {
            throw new BusinessException("이미 이 거래에 대한 후기를 작성하셨습니다.");
        }

        Review review = Review.builder()
                .item(item)
                .writer(item.getBuyer())
                .receiver(item.getSeller())
                .rating(dto.rating())
                .content(dto.content())
                .build();

        reviewRepository.save(review);
    }


    public void updateReview(Long reviewId, ReviewRequest dto, String loginUsername) {
        Review review = reviewReadService.getReviewForEdit(reviewId, loginUsername);
        // 더티 체킹 엔티티 비즈니스 메서드 호출
        review.update(dto.rating(), dto.content());
    }

    public void deleteReview(Long reviewId, String loginUsername) {
        Review review = reviewReadService.getReviewForEdit(reviewId, loginUsername);
        reviewRepository.delete(review);
    }
}