package com.example.demo.DTO.Users.Response;

import com.example.demo.DTO.Chat.Response.ChatRoomSummaryResponse;
import com.example.demo.DTO.Review.ReviewResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import org.springframework.data.domain.Page;


public record MyPageResponse (
        UserDetailResponse userInfo, // 기존 DTO 재사용
        Page<ItemSummaryResponse> sellingPosts,
        Page<ItemSummaryResponse> likedPosts,
        Page<ChatRoomSummaryResponse> buyChatRooms,
        Page<ChatRoomSummaryResponse> sellChatRooms,

        Page<ReviewResponse> writtenReviews, // 내가 쓴 후기 목록
        Page<ReviewResponse> receivedReviews  // 내가 받은 후기 목록
){}