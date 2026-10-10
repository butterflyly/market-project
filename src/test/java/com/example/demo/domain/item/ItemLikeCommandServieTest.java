package com.example.demo.domain.item;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemLike;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import com.example.demo.Service.Item.ItemLikeCommandService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Users.UserReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class)
public class ItemLikeCommandServieTest {

    @InjectMocks
    private ItemLikeCommandService itemLikeCommandService;

    @Mock
    private ItemLikeRepository itemLikeRepository;

    @Mock
    private ItemReadService itemReadService;

    @Mock
    private UserReadService userReadService;


    @Test
    void 찜하기_성공_신규등록() {
        // given
        Long itemId = 1L;
        String sellerName = "판매자";
        String buyerName = "구매자";
        Users seller = createMockUsers(sellerName);
        Users buyer = createMockUsers(buyerName);
        Item item = createMockItem(itemId, seller);

        given(itemReadService.getItemEntity(itemId)).willReturn(item);
        given(userReadService.getUserEntity(buyerName)).willReturn(buyer);
        given(itemLikeRepository.findByItemAndUsers(item, buyer)).willReturn(null); // 찜 기록 없음

        // when
        itemLikeCommandService.toggleLike(itemId, buyerName);

        // then
        verify(itemLikeRepository, times(1)).save(any(ItemLike.class));
        assertThat(item.getLikeCount()).isEqualTo(1); // 카운트 증가 확인
    }

    @Test
    void 찜하기_실패_본인상품() {
        // given
        Long itemId = 1L;
        String username = "내아이디";
        Users seller = createMockUsers(username); // 판매자와 요청자가 동일
        Item item = createMockItem(itemId, seller);

        given(itemReadService.getItemEntity(itemId)).willReturn(item);
        given(userReadService.getUserEntity(username)).willReturn(seller);

        // when & then
        assertThatThrownBy(() -> itemLikeCommandService.toggleLike(itemId, username))
                .isInstanceOf(BusinessException.class)
                .hasMessage("본인이 판매하는 상품은 찜할 수 없습니다.");
    }

    @Test
    void 찜하기_성공_찜취소() {
        // given
        Long itemId = 1L;
        String sellerName = "판매자";
        String buyerName = "구매자";
        Users seller = createMockUsers(sellerName);
        Users buyer = createMockUsers(buyerName);
        Item item = createMockItem(itemId, seller);
        ItemLike existingLike = ItemLike.builder().users(buyer).item(item).build();

        given(itemReadService.getItemEntity(itemId)).willReturn(item);
        given(userReadService.getUserEntity(buyerName)).willReturn(buyer);
        given(itemLikeRepository.findByItemAndUsers(item, buyer)).willReturn(existingLike); // 이미 찜함

        // when
        itemLikeCommandService.toggleLike(itemId, buyerName);

        // then
        verify(itemLikeRepository, times(1)).delete(existingLike);
        assertThat(item.getLikeCount()).isEqualTo(0); // 카운트 감소 확인 (초기 0 가정 시)
    }
}
