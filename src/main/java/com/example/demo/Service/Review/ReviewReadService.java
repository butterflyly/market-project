package com.example.demo.Service.Review;

import com.example.demo.DTO.Review.ReviewResponse;
import com.example.demo.Entity.Review.Review;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Review.ReviewRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewReadService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public Review getReviewForEdit(Long reviewId, String loginUsername) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 리뷰입니다."));

        if (!review.getWriter().getUsername().equals(loginUsername)) {
            throw new BusinessException("본인이 쓴 리뷰만 수정할 수 있습니다.");
        }
        return review;
    }

    /**
     * ✍ 내가 쓴 후기 목록 조회 및 DTO 변환
     */
    public Page<ReviewResponse> getWrittenReviews(String username, Pageable pageable) {
        // 1. 로그인한 유저 엔티티 조회
        Users writer = userRepository.findByUsernameAndDeletedFalse(username).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));

        // 2. 리포지토리에서 Page<Review> 엔티티 뭉치 가져오기
        Page<Review> reviewPage = reviewRepository.findByWriter(writer, pageable);

        return reviewPage.map(ReviewResponse::from);
    }

    /**
     *  내가 받은 후기 목록 조회 및 DTO 변환 (자매품)
     */
    public Page<ReviewResponse> getReceivedReviews(String username, Pageable pageable) {
        Users receiver = userRepository.findByUsernameAndDeletedFalse(username).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));

        Page<Review> reviewPage = reviewRepository.findByReceiver(receiver, pageable);

        return reviewPage.map(ReviewResponse::from);
    }

    /**
     *   판매자 고유 ID로 해당 판매자가 받은 후기 목록 조회 (타인 조회용)
     */
    public Page<ReviewResponse> getReviewsBySellerId(Long sellerId, Pageable pageable) {
        // 1. 회원 ID로 판매자 엔티티 조회 (UserReadService에 해당 메서드가 없다면 UserRepository를 직접 타거나 추가 필요)
        Users receiver = userRepository.findByIdAndDeletedFalse(sellerId).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));

        // 2. 해당 판매자(Receiver)에게 달린 리뷰 페이징 조회
        Page<Review> reviewPage = reviewRepository.findByReceiver(receiver, pageable);

        // 3. DTO 변환 후 반환
        return reviewPage.map(ReviewResponse::from);
    }
}
