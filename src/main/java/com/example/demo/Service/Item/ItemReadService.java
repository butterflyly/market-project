package com.example.demo.Service.Item;

import com.example.demo.DTO.item.Response.ItemDetailResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import com.example.demo.Repository.Review.ReviewRepository;
import com.example.demo.Repository.Users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemReadService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemCommandService itemCommandService;
    private final ItemLikeRepository itemLikeRepository;
    private final ReviewRepository reviewRepository;

    // 리스트 조회 (Page 객체로 반환)
    public Page<ItemSummaryResponse> getList(String kw, Long categoryId, Pageable pageable, String searchType) {

        // 리포지토리는 pageable 내부의 Sort 정보를 읽어서 알아서 ORDER BY 구문을 생성
        return itemRepository.findAllByKeyWord(kw, categoryId, pageable ,searchType)
                .map(ItemSummaryResponse::from);
    }

    // 특정 유저가 작성한 게시글만 페이징 조회
    public Page<ItemSummaryResponse> getItemsByUsername(String username, Pageable pageable) {

        // 1. 유저 존재 여부 확인 (또는 닉네임 등으로 조회)
        Users user = userRepository.findByUsernameAndDeletedFalse(username).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));

        // 2. 해당 유저의 게시글만 조회
        return itemRepository.findAllBySellerAndDeletedFalse(user, pageable)
                .map(ItemSummaryResponse::from);
    }

    // 특정 유저가 구매한 상품 페이징
    public Page<ItemSummaryResponse> getBoughtItemsByUsername(String username, Pageable pageable) {

        // 1. 유저 존재 여부 확인 (또는 닉네임 등으로 조회)
        Users user = userRepository.findByUsernameAndDeletedFalse(username).
                orElseThrow(() -> new BusinessException("유저가 존재하지 않습니다."));

        // 2. 해당 유저의 게시글만 조회
        return itemRepository.findAllByBuyerUsernameAndItemStatusEqualsAndDeletedFalse(user.getUsername(),ItemStatus.SOLD_OUT,pageable)
                .map(ItemSummaryResponse::from);
    }

    // 엔티티 조회
    public Item getItemEntity(Long id) {
        return itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

    }

    /**
     *  일반 사용자용 상세 조회 (조회수 증가 포함)
     */
    @Transactional // 쓰기 권한 활성화
    public ItemDetailResponse getDetailWithViews(Long id, String currentUsername, HttpServletRequest request) {

        itemCommandService.updateViews(id, request);

        return getDetail(id, currentUsername); // 아래의 순수 조회 로직 재사용
    }

    /**
     *  순수 상세 조회 (수정 폼, 관리자용 - 조회수 증가 없음)
     */
    public ItemDetailResponse getDetail(Long id, String currentUsername) {
        Item item = itemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        boolean isOwner = item.getSeller().getUsername().equals(currentUsername);

        boolean isLiked = false;
        if (currentUsername != null) {
            isLiked = itemLikeRepository.existsByItemAndUsersUsername(item, currentUsername);
        }

        // 1. 조회한 별점 결과를 변수에 담습니다.
        Double sellerRating = 0.0;

        if (item.hasActiveSeller()) {
            sellerRating = reviewRepository.getAverageRatingBySellerId(item.getSeller().getId());
        }

        return ItemDetailResponse.of(item, isOwner,isLiked , sellerRating);
    }


    /**
     * 삭제 게시글 통합 검색 (페이징 처리)
     */
    public Page<ItemSummaryResponse> getDeletedItemsForAdmin(String keyword, Pageable pageable) {
        Page<Item> items;

        if (keyword == null || keyword.trim().isEmpty()) {
            items = itemRepository.findSoftDeletedItemsAll(pageable);
        } else {
            items = itemRepository.findSoftDeletedItemsByKeyword(keyword.trim(), pageable);
        }


        return items.map(ItemSummaryResponse::from);
    }

    /**
     *  관리자 전용: 소프트 삭제된 게시글 상세 조회
     */
    public ItemDetailResponse getDeletedItemDetailForAdmin(Long id) {
        Item item = itemRepository.findSoftDeletedItemDetailById(id)
                .orElseThrow(() -> new BusinessException("존재하지 않거나 이미 영구 삭제된 게시글입니다."));

        return ItemDetailResponse.of(item, false, false,0.0);
    }

    public long getDeletedItemCount() {
        return itemRepository.countByDeletedTrue();
    }


}

