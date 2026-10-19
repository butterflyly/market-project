package com.example.demo.domain.Review;

import com.example.demo.DTO.Review.ReviewRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Review.ReviewRepository;
import com.example.demo.Service.Alram.NotificationService;
import com.example.demo.Service.Item.ItemReadService;
import com.example.demo.Service.Review.ReviewCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ReviewCommandServiceTest {

    @InjectMocks
    private ReviewCommandService reviewCommandService;

    @Mock
    private ItemReadService itemReadService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private NotificationService notificationService;

    @Test
    void 리뷰생성_성공()
    {
        // given
        Users seller = createMockUsers("판매자");
        Users buyer = createMockUsers("구매자");
        Item item = createMockItem(1L,seller,buyer);
        ReviewRequest dto = new ReviewRequest(4,"거래 잘했어요");


        given(itemReadService.getItemEntity(1L)).willReturn(item);
        given(reviewRepository.existsByItem(item)).willReturn(false);

        // when & then
        assertThatCode(() -> reviewCommandService.createReview(1L,dto,buyer.getUsername())).doesNotThrowAnyException();

    }

}
