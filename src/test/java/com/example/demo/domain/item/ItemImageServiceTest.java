package com.example.demo.domain.item;


import com.example.demo.Config.FileStorageService;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemImage;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.Item.ItemImageRepository;
import com.example.demo.Repository.Item.Item.ItemRepository;
import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Service.Item.ItemImageService;
import com.example.demo.Service.Users.UserReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.example.demo.TestFixture.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ItemImageServiceTest {

    @InjectMocks
    private ItemImageService itemImageService;

    @Mock
    private ItemImageRepository itemImageRepository;

    @Mock
    private FileStorageService fileStorageService;

    private static final String ITEM_SUB_FOLDER = "itemImageUpload/";
    private static final String ITEM_URL_PREFIX = "/itemImages/";



    // 기존 이미지가 3장 있다고 가정할 때의 헬퍼 메서드 예시
    private Item createItemWithImages(int imageCount) {
        Item item = createMockItem(1L, createMockUsers("테스트유저"));
        List<ItemImage> images = new ArrayList<>();
        for (long i = 1; i <= imageCount; i++) {
            images.add(ItemImage.builder().imageId(i).url("url" + i).item(item).build());
        }
        item.getItemImageList().addAll(images);
        return item;
    }

    private List<MultipartFile> createMockFiles(int count) {
        List<MultipartFile> files = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            files.add(new MockMultipartFile("images", "test" + i + ".jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes()));
        }
        return files;
    }

    private List<Long> createMockDeleteIds(int count) {
        List<Long> deleteIds = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            deleteIds.add(i);
        }
        return deleteIds;
    }

    // 특정 개수의 이미지를 가진 Item 생성 헬퍼
    private Item createItemWithImages(Long... imageIds) {
        Item item = createMockItem(1L, createMockUsers("테스트유저"));
        List<ItemImage> images = new ArrayList<>();
        for (Long id : imageIds) {
            images.add(ItemImage.builder().imageId(id).url("url" + id).item(item).build());
        }
        item.getItemImageList().addAll(images);
        return item;
    }

    @Test
    void 이미지_생성_성공()
    {
        // given
        Long itemId = 1L;
        String username = "테스트유저";
        Users users = createMockUsers(username);

        // 1. 상품 데이터
        Item item = createMockItem(itemId,users);

        // 2. 파일 데이터
        List<MultipartFile> tooManyImages = List.of(
                new MockMultipartFile("images", "test1.jpg", MediaType.IMAGE_JPEG_VALUE, "image1 content".getBytes()),
                new MockMultipartFile("images", "test2.jpg", MediaType.IMAGE_JPEG_VALUE, "image2 content".getBytes()),
                new MockMultipartFile("images", "test3.jpg", MediaType.IMAGE_JPEG_VALUE, "image3 content".getBytes()),
                new MockMultipartFile("images", "test4.jpg", MediaType.IMAGE_JPEG_VALUE, "image4 content".getBytes()),
                new MockMultipartFile("images", "test5.jpg", MediaType.IMAGE_JPEG_VALUE, "image5 content".getBytes()),
                new MockMultipartFile("images", "test6.jpg", MediaType.IMAGE_JPEG_VALUE, "image6 content".getBytes())
        );

        // when
        itemImageService.saveImages(item,tooManyImages);

        // then
        // 1. 파일 저장소 서비스가 이미지 개수(6개)만큼 정확히 호출되었는지 검증
        verify(fileStorageService, times(6)).
                save(any(MultipartFile.class), eq(ITEM_SUB_FOLDER), eq(ITEM_URL_PREFIX));

        // 2. 이미지 엔티티가 레포지토리를 통해 6번 저장되었는지 검증
        verify(itemImageRepository, times(6)).save(any(ItemImage.class));

    }

    @Test
    void 이미지삭제_성공() {
        // given
        Long itemId = 1L;
        Users users = createMockUsers("테스트유저");
        Item item = createMockItem(itemId, users);

        List<Long> deleteImageIds = List.of(1L, 2L);

        // 조회된 이미지 목(Mock) 객체 리스트 생성
        List<ItemImage> mockImages = List.of(
                ItemImage.builder().imageId(1L).url("url1").item(item).build(),
                ItemImage.builder().imageId(2L).url("url2").item(item).build()
        );

        given(itemImageRepository.findByImageIdInAndItem(deleteImageIds, item)).willReturn(mockImages);

        // when
        itemImageService.deleteImages(item, deleteImageIds);

        // then
        verify(fileStorageService, times(2)).delete(anyString(), eq(ITEM_SUB_FOLDER), eq(ITEM_URL_PREFIX));
        assertThat(item.getItemImageList()).isEmpty(); // 리스트에서 제거되었는지 확인
    }

    @Test
    void 이미지삭제_실패_존재하지않는이미지() {
        // given
        Long itemId = 1L;
        Users users = createMockUsers("테스트유저");
        Item item = createMockItem(itemId, users);

        List<Long> deleteImageIds = List.of(1L, 999L); // 999L은 DB에 없는 ID라고 가정

        // 요청은 2개를 보냈으나, DB에서는 1개만 조회된 상황
        List<ItemImage> mockImages = List.of(
                ItemImage.builder().imageId(1L).url("url1").item(item).build()
        );

        given(itemImageRepository.findByImageIdInAndItem(deleteImageIds, item)).willReturn(mockImages);

        // when & then
        assertThatThrownBy(() -> itemImageService.deleteImages(item, deleteImageIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 상품에 속하지 않은 고유 번호가 삭제 요청에 포함되어 있습니다.");

        // 파일 삭제나 리스트 제거가 일어나면 안 됨
        verify(fileStorageService, never()).delete(anyString(), anyString(), anyString());
    }

    @Test
    void 이미지검증_성공()
    {
        // given (기존 3장 - 삭제 1장 + 추가 2장 = 4장 <= 5장)
        Item item = createItemWithImages(3);
        List<Long> deleteIds = createMockDeleteIds(1);
        List<MultipartFile> addFiles = createMockFiles(2);

        // when & then (예외 없이 통과)
        assertThatCode(() -> itemImageService.validateImageCount(item, addFiles, deleteIds))
                .doesNotThrowAnyException();
    }

    @Test
    void 이미지검증_실패()
    {
        // given (기존 2장인데 3장을 삭제하려는 경우)
        Item item = createItemWithImages(2);
        List<Long> deleteIds = createMockDeleteIds(3);
        List<MultipartFile> addFiles = createMockFiles(1);

        // when & then
        assertThatThrownBy(() -> itemImageService.validateImageCount(item, addFiles, deleteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("삭제 요청 이미지 수가 현재 존재하는 이미지 수보다 많습니다.");
    }

    @Test
    void 이미지_삭제_검증_성공() {
        // given (기존 이미지 ID: 1, 2, 3 / 요청한 삭제 ID: 1, 3)
        Item item = createItemWithImages(1L, 2L, 3L);
        List<Long> deleteIds = List.of(1L, 3L);

        // when & then (예외 없이 통과)
        assertThatCode(() -> itemImageService.validateDeleteImages(item, deleteIds))
                .doesNotThrowAnyException();
    }

    @Test
    void 이미지_삭제_검증_실패_삭제할이미지없음() {
        // given (기존 이미지가 비어있는 경우)
        Item item = createItemWithImages(); // 이미지 없음
        List<Long> deleteIds = List.of(1L);

        // when & then
        assertThatThrownBy(() -> itemImageService.validateDeleteImages(item, deleteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 상품에 속하지 않은 고유 번호가 삭제 요청에 포함되어 있습니다.");
    }

    @Test
    void 이미지_삭제_검증_실패_상품에속하지않은이미지() {
        // given (기존 이미지 ID: 1, 2 / 요청한 삭제 ID에 999(존재하지 않거나 다른 상품의 이미지) 포함)
        Item item = createItemWithImages(1L, 2L);
        List<Long> deleteIds = List.of(1L, 999L);

        // when & then
        assertThatThrownBy(() -> itemImageService.validateDeleteImages(item, deleteIds))
                .isInstanceOf(BusinessException.class)
                .hasMessage("해당 상품에 속하지 않은 고유 번호가 삭제 요청에 포함되어 있습니다.");
    }

}
