package com.example.demo.domain.item;


import com.example.demo.DTO.Alram.DealConfirmedEvent;
import com.example.demo.DTO.Alram.ItemStatusChangedEvent;
import com.example.demo.DTO.item.Request.ItemCreateRequest;
import com.example.demo.DTO.item.Request.ItemUpdateRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;

import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Service.Item.ItemCategoryService;
import com.example.demo.Service.Item.ItemCommandService;
import com.example.demo.Service.Item.ItemImageService;
import com.example.demo.Service.Users.UserReadService;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class ItemCommandServiceTest {

    @InjectMocks
    private ItemCommandService itemCommandService;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserReadService userReadService;

    @Mock
    private ItemCategoryService itemCategoryService;

    @Mock
    private ItemImageService itemImageService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ApplicationEventPublisher eventPublisher; // 스프링 내장 확성기


    @Test
    void 상품_생성() throws IOException {
        // given
        // 1. 필요 변수 및 객체 생성
        String username = "정상유저";
        Users mockUser = createMockUsers(username); // 경고 0회 유저
        ItemCreateRequest request = new ItemCreateRequest("제목", 10000,
                "내용", 1L, null);
        ItemCategory category = ItemCategory.builder().id(1L).name("디지털기기").build();

        // 2. 유저 및 카테고리 스터빙
        given(userReadService.getUserEntity(username)).willReturn(mockUser);
        given(itemCategoryService.findByCategory(request.categoryId())).willReturn(category);

        // when
        itemCommandService.createItem(request, username);

        // then
        verify(userReadService, times(1)).getUserEntity(username);
        verify(itemRepository, times(1)).save(any(Item.class));
        verify(itemImageService, times(1)).saveImages(any(Item.class), eq(request.images()));
    }

    @Test
    void 상품_생성_실패_블랙유저()
    {
        // given
        String username = "블랙유저";
        Users mockUser = createMockUsers(username ,2); // 경고 2회 유저
        ItemCreateRequest request = new ItemCreateRequest("제목", 10000,
                "내용", 1L, null);

        given(userReadService.getUserEntity(username)).willReturn(mockUser);

        // when & then
        assertThatThrownBy(() -> itemCommandService.createItem(request,username))
                .isInstanceOf(BusinessException.class)
                .hasMessage("경고 2회 이상 누적으로 인해 현재 게시글 작성이 제한된 상태입니다.");

        // 유저가 경고 유저므로 카테고리 조회 및 데이터 생성이 일어나지 않음
        verify(itemCategoryService , never()).findByCategory(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void 상품생성_실패_이미지초과()
    {
        // given
        String username = "정상유저";
        Users mockUser = createMockUsers(username); // 경고 0회 유저

        //  1. 6장의 더미 MockMultipartFile 생성 (이름, 오리지널파일명, contentType, 바이트배열) 및 dto 생성
        List<MultipartFile> tooManyImages = List.of(
                new MockMultipartFile("images", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, "image1 content".getBytes()),
                new MockMultipartFile("images", "test2.jpg", MediaType.IMAGE_JPEG_VALUE, "image2 content".getBytes()),
                new MockMultipartFile("images", "test3.jpg", MediaType.IMAGE_JPEG_VALUE, "image3 content".getBytes()),
                new MockMultipartFile("images", "test4.jpg", MediaType.IMAGE_JPEG_VALUE, "image4 content".getBytes()),
                new MockMultipartFile("images", "test5.jpg", MediaType.IMAGE_JPEG_VALUE, "image5 content".getBytes()),
                new MockMultipartFile("images", "test6.jpg", MediaType.IMAGE_JPEG_VALUE, "image6 content".getBytes())
        );

        ItemCreateRequest request = new ItemCreateRequest("제목", 10000, "내용", 1L, tooManyImages);

        // 2. 유저 스터빙
        given(userReadService.getUserEntity(username)).willReturn(mockUser);

        // when & then
        assertThatThrownBy(() -> itemCommandService.createItem(request, username))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미지는 최대 5장까지 업로드할 수 있습니다.");

        // 이미지가 초과되었으므로 카테고리 조회 및 데이터 생성이 일어나지 않음
        verify(itemCategoryService , never()).findByCategory(any());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void 데이터수정_성공() throws IOException {
        // given
        Long itemId = 1L;
        String username = "작성자유저";

        // 1. 작성자 유저 생성
        Users seller = createMockUsers(username);

        // 2. 수정 대상인 기존 Item 가짜 객체 생성 (판매자가 위 유저여야 권한 검증 통과)
        Item item = createMockItem(itemId,seller);

        ItemUpdateRequest request = new ItemUpdateRequest("수정 제목", 15000, "수정 내용", 2L, List.of());
        List<Long> deleteIds = List.of();
        ItemCategory newCategory = ItemCategory.builder().id(2L).name("가구").build();

        // 3. findItem()이 내부적으로 호출하는 repository 스터빙
        given(itemRepository.findByIdAndDeletedFalse(itemId)).willReturn(Optional.of(item));

        // 4. 협력 서비스의 성공 가정 스터빙
        given(itemCategoryService.findByCategory(request.categoryId())).willReturn(newCategory);

        // when
        itemCommandService.updateItem(itemId,request,username,deleteIds);

        // then
        assertThat(item.getTitle()).isEqualTo("수정 제목");
        assertThat(item.getPrice()).isEqualTo(15000);

        verify(itemRepository, times(1)).findByIdAndDeletedFalse(itemId);
        verify(itemImageService, times(1)).validateDeleteImages(item, deleteIds);
        verify(itemImageService, times(1)).validateImageCount(item, request.images(), deleteIds);
        verify(itemCategoryService, times(1)).findByCategory(request.categoryId());
        verify(itemRepository, times(1)).save(item);
        verify(itemImageService, times(1)).deleteImages(item, deleteIds);
        verify(itemImageService, times(1)).saveImages(item, request.images());

    }

    @Test
    void 상품수정_실패_삭제이미지유효성검증실패()
    {
        // given
        Long itemId = 1L;
        String username = "작성자유저";
        Users seller = createMockUsers(username);

        Item item = createMockItem(itemId,seller);

        ItemUpdateRequest request = new ItemUpdateRequest("수정제목", 10000, "내용", 2L, List.of());
        List<Long> deleteIds = List.of(999L); // 잘못된 삭제 이미지 ID

        given(itemRepository.findByIdAndDeletedFalse(itemId)).willReturn(Optional.of(item));

        // 내부 구현은 몰라도, 이 메서드가 호출되면 BusinessException을 던지도록 설정
        willThrow(new BusinessException("삭제 요청 이미지 수가 현재 존재하는 이미지 수보다 많습니다."))
                .given(itemImageService).validateDeleteImages(item, deleteIds);

        // when & then
        assertThatThrownBy(() -> itemCommandService.updateItem(itemId, request, username, deleteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("삭제 요청 이미지 수가 현재 존재하는 이미지 수보다 많습니다.");

        // 이미지 검증 단계에서 예외가 터졌으므로, 카테고리 조회나 save는 절대 실행되면 안 됨
        verify(itemCategoryService, never()).findByCategory(any());
        verify(itemRepository, never()).save(any());
        verify(itemImageService, never()).deleteImages(any(), any());
    }

    @Test
    void 상품삭제_성공()
    {
        // given
        Long itemId = 1L;
        String username = "작성자유저";
        Users seller = createMockUsers(username);

        Item item = createMockItem(itemId,seller);

        // 상품 스터빙
        given(itemRepository.findByIdAndDeletedFalse(itemId)).willReturn(Optional.of(item));

        // when
        itemCommandService.deleteItem(itemId, username);

        // then
        assertThat(item.isDeleted()).isTrue();
        verify(itemRepository , times(1)).findByIdAndDeletedFalse(itemId);
    }

    @Test
    void 상품삭제_실패()
    {
        // given
        Long itemId = 1L;
        String username = "작성자유저";
        String fUsername = "가짜이름";
        Users seller = createMockUsers(username);

        Item item = createMockItem(itemId,seller);

        // 상품 스터빙
        given(itemRepository.findByIdAndDeletedFalse(itemId)).willReturn(Optional.of(item));

        // when & then
        assertThatThrownBy(() -> itemCommandService.deleteItem(itemId,fUsername))
                .isInstanceOf(BusinessException.class)
                .hasMessage("삭제 권한이 없습니다.");

        assertThat(item.isDeleted()).isFalse();
    }

    @Test
    void 조회수_증가_성공()
    {
        // given
        Long itemId = 1L;
        String clientIp = "127.0.0.1";
        String redisKey = "item:views:" + itemId;

        // 1. HttpServletRequest가 특정 IP를 반환하도록 스터빙
        given(httpServletRequest.getHeader("X-Forwarded-For")).willReturn(clientIp);

        // 2. redisTemplate.opsForSet()이 mock으로 만든 setOperations를 반환하도록 스터빙
        //   NullPointerException을 리턴하는게 아닌 가짜(Mock) Set 연산 객체(setOperations)를 리턴
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // Redis Set에 추가했을 때 처음이므로 1L을 리턴한다고 가정
        // SADD 명령어는 데이터가 처음 들어갈 때만 1L을 반환
        given(setOperations.add(anyString(), anyString())).willReturn(1L);

        // when
        itemCommandService.updateViews(itemId,httpServletRequest);

        // then
        // 1. DB 조회수 증가 쿼리가 실행되었는지 검증
        verify(itemRepository, times(1)).updateView(itemId);

        // 2. 24시간 만료 시간(Expire)이 설정되었는지 검증
        verify(redisTemplate, times(1)).expire(anyString(), any(Duration.class));
    }

    @Test
    void 조회수_증가_실패()
    {
        // given
        Long itemId = 1L;
        String clientIp = "127.0.0.1";
        String redisKey = "item:views:" + itemId;

        given(httpServletRequest.getHeader("X-Forwarded-For")).willReturn(clientIp);
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // 이미 Redis Set에 존재하므로 0L(또는 null)을 리턴한다고 가정
        given(setOperations.add(anyString(), anyString())).willReturn(0L);

        // when
        itemCommandService.updateViews(itemId, httpServletRequest);

        // then
        // DB 조회수 증가나 만료 시간 설정이 일어나면 안 됨
        verify(itemRepository, never()).updateView(any());
        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    void 판마상태_변경_성공()
    {
        // given
        Long itemId = 1L;
        String sellerusername = "작성자유저";
        String buyerusername = "구매유저";
        Users seller = createMockUsers(sellerusername);
        Users buyer = createMockUsers(buyerusername);

        Item item = createMockItem(itemId,seller);

        given(itemRepository.findByIdAndDeletedFalse(itemId)).willReturn(Optional.of(item));

        given(userReadService.getUserEntity(buyerusername)).willReturn(buyer);

        // when
        itemCommandService.updateStatus(itemId,ItemStatus.RESERVED,sellerusername,buyerusername);

        // then
        // 1. 상태와 구매자가 잘 변경되었는지 확인
        assertThat(item.getItemStatus()).isEqualTo(ItemStatus.RESERVED);
        assertThat(item.getBuyer()).isEqualTo(buyer);

        // 2. 협력 객체들이 잘 호출되었는가?
        verify(itemRepository, times(1)).findByIdAndDeletedFalse(itemId);
        verify(userReadService, times(1)).getUserEntity(buyerusername);
        verify(eventPublisher, times(1)).publishEvent(any(ItemStatusChangedEvent.class));
    }

    @Test
    void 판매상태_변경_실패_구매자누락() {
        // given
        Long itemId = 1L;
        String sellerUsername = "작성자유저";
        Users seller = createMockUsers(sellerUsername);
        Item item = createMockItem(itemId, seller);

        given(itemRepository.findByIdAndDeletedFalse(itemId)).willReturn(Optional.of(item));

        // when & then (RESERVED 상태로 바꾸는데 buyerUsername을 null이나 빈 값으로 전달)
        assertThatThrownBy(() -> itemCommandService.updateStatus(itemId, ItemStatus.RESERVED,
                sellerUsername, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RESERVED 처리 시 대상자 지정은 필수입니다.");

        // 상태가 변하지 않아야 함
        assertThat(item.getItemStatus()).isEqualTo(ItemStatus.ON_SALE); // 기본 상태 가정
        verify(userReadService, never()).getUserEntity(any());
    }
}





