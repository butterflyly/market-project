package com.example.demo.domain.item;

import com.example.demo.Config.FileStorageService;
import com.example.demo.DTO.item.Request.ItemUpdateRequest;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Entity.Item.ItemImage;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Repository.Item.Item.ItemCategoryRepository;
import com.example.demo.Repository.Item.Item.ItemImageRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Item.ItemCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest
@Transactional
public class ItemServiceIntegrationTest {

    @Autowired
    private ItemCommandService itemCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemImageRepository itemImageRepository;

    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private SetOperations<String, String> setOperations; // opsForSet()이 리턴하는 객체



    @BeforeEach
    void setUp() {
        // fileStorageService.save()가 호출되면 무조건 가짜 URL 문자열을 리턴하도록 설정!
        given(fileStorageService.save(any(), any(), any()))
                .willReturn("/itemImages/mock-uuid-test.png");
    }

    @BeforeEach
    void setUpRedisMock() {
        // 1. redisTemplate.opsForSet()이 호출되면 가짜 SetOperations를 리턴하도록 설정
        given(redisTemplate.opsForSet()).willReturn(setOperations);

        // 2. setOperations.add(...)가 호출되면 처음 방문(1L)했다고 가정하고 1L 리턴
        given(setOperations.add(anyString(), anyString())).willReturn(1L);
    }

    @Test
    void 게시글수정_성공() throws IOException {

        // given
        Users seller = createMockUsersTotal("seller");
        userRepository.save(seller);

        ItemCategory itemCategory = ItemCategory.builder()
                .id(1L)
                .name("가전")
                .build();
        itemCategoryRepository.save(itemCategory);

        // 1. 이미지를 담을 수 있는 Item 객체 생성 및 저장
        Item item = createMockItem(null, seller, new ArrayList<>());
        itemRepository.save(item);

        // 2. ItemImage 생성 시 아이템과 확실하게 엮어주기
        ItemImage mockImage = ItemImage.builder()
                .url("/images/test.png")
                .item(item)
                .build();
        itemImageRepository.save(mockImage);

        // 3. 양방향 리스트에도 추가
        item.getItemImageList().add(mockImage);

        // 가짜 MultipartFile 5개 생성
        List<MultipartFile> mockFiles = List.of(
                new MockMultipartFile("images", "test1.png", "image/png", "fake-image-content-1".getBytes()),
                new MockMultipartFile("images", "test2.png", "image/png", "fake-image-content-2".getBytes()),
                new MockMultipartFile("images", "test3.png", "image/png", "fake-image-content-3".getBytes()),
                new MockMultipartFile("images", "test4.png", "image/png", "fake-image-content-4".getBytes()),
                new MockMultipartFile("images", "test5.png", "image/png", "fake-image-content-5".getBytes())
        );

        ItemUpdateRequest request = new ItemUpdateRequest(
                "수정제목", 150000, "수정내용", itemCategory.getId(), mockFiles
        );

        // when
        itemCommandService.updateItem(item.getId(), request, "seller", List.of(mockImage.getImageId()));

        // then
        assertThat(item).isNotNull();
        assertThat(item.getPrice()).isEqualTo(150000);
        assertThat(item.getItemImageList()).isEmpty();
    }

    @Test
    void 조회수증가_성공()
    {
        // given
        // 1. HTTPServlet 값 모킹
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getHeader("X-Forwarded-For")).willReturn("127.0.0.1");

        Users seller = createMockUsersTotal("seller");
        userRepository.save(seller);

        Item item = createMockItemTotal(seller,null);
        itemRepository.save(item);

        // when
        itemCommandService.updateViews(item.getId(),request);

        // then
        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();

        assertThat(updatedItem.getViews()).isEqualTo(1);
    }

    @Test
    void 판매상태변경_성공()
    {
        // given
        Users seller = createMockUsersTotal("seller");
        userRepository.save(seller);

        Users buyer = createMockUsersTotal2("buyer");
        userRepository.save(buyer);

        Item item = createMockItemTotal(seller,buyer);
        itemRepository.save(item);

        // when
        itemCommandService.updateStatus(item.getId(), ItemStatus.SOLD_OUT,"seller","buyer");

        // then
        Item updateItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updateItem.getItemStatus()).isEqualTo(ItemStatus.SOLD_OUT);
    }

}
