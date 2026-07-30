package com.example.demo.Controller.Review;

import com.example.demo.DTO.Review.ReviewResponse;
import com.example.demo.DTO.Users.Response.UserDetailResponse;
import com.example.demo.Entity.Review.Review;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Service.Review.ReviewCommandService;
import com.example.demo.Service.Review.ReviewReadService;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reviews")
public class ReviewViewController {

    private final ReviewReadService reviewReadService;
    private final UserReadService userReadService;

    //  리뷰 작성 화면
    @GetMapping("/write")
    public String writeForm(@RequestParam Long itemId, Model model) {
        model.addAttribute("itemId", itemId);
        return "review/write";
    }

    // ️ 리뷰 수정 화면
    @GetMapping("/edit/{reviewId}")
    public String editForm(@PathVariable Long reviewId, Model model, @AuthenticationPrincipal UserDetails userDetails) {
        Review review = reviewReadService.getReviewForEdit(reviewId, userDetails.getUsername());
        model.addAttribute("review", review);
        return "review/edit";
    }

    @GetMapping("/seller/{sellerId}")
    public String sellerReviewList(
            @PathVariable Long sellerId,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        // 1. 후기 주인(판매자)의 정보 조회 (화면 상단 프로필용)
        // 예시: userReadService에 있는 유저 DTO나 엔티티 조회 메서드 활용
        UserDetailResponse targetUser = userReadService.getUserResponseDTOById(sellerId);

        // 2. 이 판매자가 받은 후기 리스트 DTO 뭉치 가져오기
        Page<ReviewResponse> reviews = reviewReadService.getReviewsBySellerId(sellerId, pageable);

        // 3. 화면에 배달
        model.addAttribute("targetUser", targetUser);
        model.addAttribute("reviews", reviews);

        return "review/list"; // 이전에 만들었던 오픈 광장형 list.html로 매핑
    }

}