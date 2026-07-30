package com.example.demo.Controller.Review;

import com.example.demo.DTO.CommonResponse;
import com.example.demo.DTO.Review.ReviewRequest;
import com.example.demo.Service.Review.ReviewCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewApiController {

    private final ReviewCommandService reviewCommandService;


    @PostMapping("/item/{itemId}")
    public ResponseEntity<CommonResponse<?>> createReview(
            @PathVariable Long itemId,
            @Valid @RequestBody ReviewRequest dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewCommandService.createReview(itemId, dto, userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "리뷰가 등록되었습니다.", null));
    }


    @PatchMapping("/{reviewId}")
    public ResponseEntity<CommonResponse<?>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewCommandService.updateReview(reviewId, dto, userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "리뷰가 수정되었습니다.", null));
    }


    @DeleteMapping("/{reviewId}")
    public ResponseEntity<CommonResponse<?>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewCommandService.deleteReview(reviewId, userDetails.getUsername());
        return ResponseEntity.ok(new CommonResponse<>(true, "리뷰가 삭제되었습니다.", null));
    }
}
