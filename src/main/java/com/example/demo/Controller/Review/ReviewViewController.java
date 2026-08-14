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


    // 1. 판매자 거래 후기 모아보기 (공개 페이지용)
    @GetMapping("/seller/{sellerId}")
    public String sellerReviewList(
            @PathVariable Long sellerId,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        UserDetailResponse targetUser = userReadService.getUserResponseDTOById(sellerId);
        Page<ReviewResponse> reviews = reviewReadService.getReviewsBySellerId(sellerId, pageable);

        model.addAttribute("targetUser", targetUser);
        model.addAttribute("reviews", reviews);

        return "review/seller-list";
    }

    // 2. 내가 작성한 후기 목록 (마이페이지 연동용)
    @GetMapping("/my/written")
    public String myWrittenReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        Page<ReviewResponse> reviews = reviewReadService.getWrittenReviews(userDetails.getUsername(), pageable);

        model.addAttribute("type", "written");
        model.addAttribute("reviews", reviews);

        return "review/my-reviews";
    }

    // 3. 내가 받은 후기 목록 (마이페이지 연동용)
    @GetMapping("/my/received")
    public String myReceivedReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 10) Pageable pageable,
            Model model) {

        Page<ReviewResponse> reviews = reviewReadService.getReceivedReviews(userDetails.getUsername(), pageable);

        model.addAttribute("type", "received");
        model.addAttribute("reviews", reviews);

        return "review/my-reviews";
    }
}