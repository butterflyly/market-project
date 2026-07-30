package com.example.demo.Service.Users;

import com.example.demo.DTO.Chat.Response.ChatRoomSummaryResponse;
import com.example.demo.DTO.Review.ReviewResponse;
import com.example.demo.DTO.Users.Response.MyPageResponse;
import com.example.demo.DTO.Users.Response.UserDetailResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Service.Chat.ChatRoomReadService;
import com.example.demo.Service.Item.ItemLikeReadService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Review.ReviewReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageReadService {

    // 💡 마이페이지에 필요한 모든 읽기 전용 서비스들을 일렬로 주입받습니다.
    private final UserReadService userReadService;
    private final ItemReadService itemReadService;
    private final ItemLikeReadService itemLikeReadService;
    private final ChatRoomReadService chatRoomReadService;
    private final ReviewReadService reviewReadService;

    /**
     *  마이페이지 전체 데이터 조립 통합 메서드
     */
    public MyPageResponse getMyPageData(String username, Pageable pageable) {

        // 1. 순수 유저 정보 조회 (UserReadService 이용)
        Users user = userReadService.getUserEntity(username);
        UserDetailResponse userInfo = UserDetailResponse.of(user);

        // 2. 상품, 찜, 채팅, 리뷰 데이터 평화롭게 가져오기 (각각의 서비스 호출)
        Page<ItemSummaryResponse> sellingPosts = itemReadService.getItemsByUsername(username, pageable);

        Page<ItemSummaryResponse> likedPosts = itemLikeReadService.getLikedItemsByUsername(username, pageable);

        Page<ChatRoomSummaryResponse> buyChatRooms = chatRoomReadService.getChatRooms(
                username, "BUYER", pageable.getPageNumber(), pageable.getPageSize());

        Page<ChatRoomSummaryResponse> sellChatRooms = chatRoomReadService.getChatRooms(
                username, "SELLER", pageable.getPageNumber(), pageable.getPageSize());

        Page<ReviewResponse> writtenReviews = reviewReadService.getWrittenReviews(username, pageable);
        Page<ReviewResponse> receivedReviews = reviewReadService.getReceivedReviews(username, pageable);

        // 3. 하나의 거대한 마이페이지 DTO로 패킹해서 반환
        return new MyPageResponse(
                userInfo,
                sellingPosts,
                likedPosts,
                buyChatRooms,
                sellChatRooms,
                writtenReviews,
                receivedReviews
        );
    }
}