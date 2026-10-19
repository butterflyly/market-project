package com.example.demo.domain.item;

import com.example.demo.DTO.item.Response.ItemDetailResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.Item.ItemCategoryRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import com.example.demo.Repository.Review.ReviewRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Item.ItemCommandService;
import com.example.demo.Service.Item.ItemReadService;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
public class ItemReadServiceTest {

    @InjectMocks
    private ItemReadService itemReadService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;


    @Mock
    private ItemLikeRepository itemLikeRepository;

    @Mock
    private ReviewRepository reviewRepository;


    // 가짜 유저 헬퍼 메소드
    private Users createMockUsers(String username)
    {
        return Users.builder()
                .id(1L)
                .username(username)
                .build();
    }

    // 가짜 상품 헬퍼 메소드
    private Page<Item> createMockItemPage(String username, Pageable pageable) {
        return createMockItemPage(username, null, ItemStatus.ON_SALE, pageable);
    }

    private Page<Item> createMockItemPage(String sellerUsername, Users buyer, ItemStatus status, Pageable pageable) {
        Users seller = createMockUsers(sellerUsername);
        ItemCategory category = ItemCategory.builder().id(1L).name("디지털기기").build();

        Item item1 = Item.builder()
                .id(100L)
                .title("테스트 상품 1")
                .price(500000)
                .seller(seller)
                .buyer(buyer)
                .itemCategory(category)
                .itemStatus(status)
                .build();

        Item item2 = Item.builder()
                .id(100L)
                .title("테스트 상품 1")
                .price(500000)
                .seller(seller)
                .buyer(buyer)
                .itemCategory(category)
                .itemStatus(status)
                .build();


        return new PageImpl<>(List.of(item1,item2), pageable, 1);
    }

    @Test
    void 리스트_조회()
    {
        // given
        String kw = "";
        Long categoryId = 1L;
        String searchType = "title";
        Pageable pageable = PageRequest.of(0, 10);
        String username = "테스트유저네임";

        Page<Item> itemPage = createMockItemPage(username,pageable);

        given(itemRepository.findAllByKeyWord(kw,categoryId,pageable,searchType)).willReturn(itemPage);

        // when
        Page<ItemSummaryResponse> result = itemReadService.getList(kw,categoryId,pageable,searchType);

        // then
        // 1. 결과가 Null이 아닌가
        assertThat(result).isNotNull();

        // 2. 반환된 개수가 PageImpl에 넣은 2개와 일치하는가
        assertThat(result.getContent()).hasSize(2);

        // 3. Entity -> DTO 매핑(변환)이 정상적으로 이루어졌는지 필드값 검증
        assertThat(result.getContent().get(0).title()).isEqualTo("테스트 상품 1");
        assertThat(result.getContent().get(0).price()).isEqualTo(500000);

        // 4. Repository의 조회 메서드가 지정한 파라미터로 1회 호출되었는가
        verify(itemRepository, times(1)).findAllByKeyWord(kw, categoryId, pageable, searchType);
    }

    @Test
    void 유저상품_조회() {
        // given
        String username = "테스터유저네임";
        Pageable pageable = PageRequest.of(0, 10);


        Users mockUser = createMockUsers(username);
        Page<Item> mockItemPage = createMockItemPage(username, pageable);

        given(userRepository.findByUsernameAndDeletedFalse(username))
                .willReturn(Optional.of(mockUser));
        given(itemRepository.findAllBySellerAndDeletedFalse(mockUser, pageable))
                .willReturn(mockItemPage);

        // when
        Page<ItemSummaryResponse> result = itemReadService.getItemsByUsername(username, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).sellerUsername()).isEqualTo(username);

        verify(userRepository, times(1)).findByUsernameAndDeletedFalse(username);
        verify(itemRepository, times(1)).findAllBySellerAndDeletedFalse(mockUser, pageable);
    }

    @Test
    void 유저상품_조회_작성한_게시글_없음()
    {
        // given
        String targetUsername = "테스터2"; // 검색 대상 유저 (게시글을 작성하지 않은 유저)
        Pageable pageable = PageRequest.of(0, 10);

        // 1. 테스터2 유저 객체 생성
        Users mockUser2 = createMockUsers(targetUsername);

        // 2. 유저는 존재하므로 mockUser2 반환
        given(userRepository.findByUsernameAndDeletedFalse(targetUsername))
                .willReturn(Optional.of(mockUser2));

        // 3. 테스터2가 작성한 상품은 없으므로 빈 Page 반환
        given(itemRepository.findAllBySellerAndDeletedFalse(mockUser2, pageable))
                .willReturn(Page.empty(pageable));

        // when
        Page<ItemSummaryResponse> result = itemReadService.getItemsByUsername(targetUsername, pageable);

        // then
        // 1. null 검증 (객체 자체가 생성되어 반환되었는가?)
        assertThat(result).isNotNull();

        // 2. empty 검증 (객체는 살아있지만, 그 안의 데이터 개수가 0개인가?)
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);

        // 3. Repository 호출 검증
        verify(userRepository, times(1)).findByUsernameAndDeletedFalse(targetUsername);
        verify(itemRepository, times(1)).findAllBySellerAndDeletedFalse(mockUser2, pageable);
    }

    @Test
    void 유저상품_조회_실패_유저없음() {

        // given
        String username = "존재하지않는유저";
        Pageable pageable = PageRequest.of(0, 10);

        // 유저가 존재하지 않는 상황 설정 (Optional.empty 반환)
        given(userRepository.findByUsernameAndDeletedFalse(username))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> itemReadService.getItemsByUsername(username, pageable))
                .isInstanceOf(BusinessException.class)
                .hasMessage("유저가 존재하지 않습니다.");

        // 유저 조회만 실행되고, 상품 조회 repository는 호출되지 않았는지 검증
        verify(userRepository, times(1)).findByUsernameAndDeletedFalse(username);
        verify(itemRepository, never()).findAllBySellerAndDeletedFalse(any(), any());
    }

    @Test
    void 구매목록_조회_성공()
    {
        // given
        String buyerUsername = "구매자유저네임";
        Pageable pageable = PageRequest.of(0, 10);

        Users buyer = createMockUsers(buyerUsername);
        String sellerUsername = "판매자";

        Page<Item> itemPage = createMockItemPage(sellerUsername,buyer,ItemStatus.SOLD_OUT,pageable);

        given(userRepository.findByUsernameAndDeletedFalse(buyerUsername))
                .willReturn(Optional.of(buyer));

        given(itemRepository.
                findAllByBuyerUsernameAndItemStatusEqualsAndDeletedFalse(buyerUsername,ItemStatus.SOLD_OUT,pageable))
                .willReturn(itemPage);

        // when
        Page<ItemSummaryResponse> result = itemReadService.getBoughtItemsByUsername(buyerUsername, pageable);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).itemStatus()).isEqualTo(ItemStatus.SOLD_OUT);

        verify(userRepository, times(1)).findByUsernameAndDeletedFalse(buyerUsername);
        verify(itemRepository, times(1))
                .findAllByBuyerUsernameAndItemStatusEqualsAndDeletedFalse(buyerUsername, ItemStatus.SOLD_OUT, pageable);

    }

    @Test
    void 상세조회_성공_타인조회_좋아요함() {
        // given
        Long itemId = 100L;
        String currentUsername = "로그인유저";
        String sellerUsername = "판매자유저";
        Pageable pageable = PageRequest.of(0, 10);

        Page<Item> mockItemPage = createMockItemPage(sellerUsername, pageable);
        Item mockItem = mockItemPage.getContent().get(0);

        given(itemRepository.findByIdAndDeletedFalse(itemId))
                .willReturn(Optional.of(mockItem));

        given(itemLikeRepository.existsByItemAndUsersUsername(mockItem, currentUsername))
                .willReturn(true);

        given(reviewRepository.getAverageRatingBySellerId(mockItem.getSeller().getId()))
                .willReturn(4.5);

        // when
        ItemDetailResponse result = itemReadService.getDetail(itemId, currentUsername);

        // then
        assertThat(result).isNotNull();
        assertThat(result.title()).isEqualTo("테스트 상품 1");
        assertThat(result.isOwner()).isFalse();
        assertThat(result.isLiked()).isTrue();
        assertThat(result.sellerRating()).isEqualTo(4.5);

        verify(itemRepository, times(1)).findByIdAndDeletedFalse(itemId);
        verify(itemLikeRepository, times(1)).existsByItemAndUsersUsername(mockItem, currentUsername);
        verify(reviewRepository, times(1)).getAverageRatingBySellerId(mockItem.getSeller().getId());
    }

}
