package com.example.demo.Service.Item;

import com.example.demo.Config.FileStorageService;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemImage;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.Item.ItemImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemImageService {

    private final ItemImageRepository itemImageRepository;
    private final FileStorageService fileStorageService;

    // properties 파일 구조 개선으로 하위 폴더명과 URL 접두사만 상수로 관리
    private static final String ITEM_SUB_FOLDER = "itemImageUpload/";
    private static final String ITEM_URL_PREFIX = "/itemImages/";

    /**
     * 상품 이미지 다중 저장 및 DB 기록
     */
    public void saveImages(Item item, List<MultipartFile> files) {

        // 파일이 없는 경우 리턴
        if (files == null) return;

        // 파일 for 문으로 찾아서 저장
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            // 파일 저장소 서비스 호출
            String savedUrl = fileStorageService.save(file, ITEM_SUB_FOLDER, ITEM_URL_PREFIX);

            // 엔티티 생성 및 관계 편입
            ItemImage image = ItemImage.builder()
                    .url(savedUrl)
                    .item(item)
                    .build();

            itemImageRepository.save(image);
        }
    }

    /**
     * 상품 이미지 삭제 로직
     */
    public void deleteImages(Item item, List<Long> deleteImageIds) {
        if (deleteImageIds == null || deleteImageIds.isEmpty()) return;

        // Repository의 수정한 메서드명(ByImageId)에 맞춰 호출
        List<ItemImage> images = itemImageRepository.findByImageIdInAndItem(deleteImageIds, item);

        // 물리적 파일 디스크에서 삭제
        images.forEach(img -> fileStorageService.delete(img.getUrl(), ITEM_SUB_FOLDER, ITEM_URL_PREFIX));

        // 부모 객체의 리스트에서 제거하여 cascade 및 orphanRemoval에 의해 DB 데이터 자동 삭제 유도
        item.getItemImageList().removeAll(images);
    }

    /**
     * 이미지 개수 검증 (실제 비어있지 않은 파일만 카운트)
     */
    @Transactional(readOnly = true)
    public void validateImageCount(Item post, List<MultipartFile> files, List<Long> deleteImageIds) {
        int existing = post.getItemImageList() != null ? post.getItemImageList().size() : 0;
        int delete = deleteImageIds != null ? deleteImageIds.size() : 0;

        // 업로드 파일 중 실제로 알맹이가 있는 파일만 개수로 인정
        int add = 0;
        if (files != null) {
            add = (int) files.stream().filter(f -> f != null && !f.isEmpty()).count();
        }

        if (delete > existing) {
            throw new BusinessException("삭제 요청 이미지 수가 현재 존재하는 이미지 수보다 많습니다.");
        }

        if (existing - delete + add > 5) {
            throw new BusinessException("이미지는 최대 5장까지 업로드할 수 있습니다.");
        }
    }

    /**
     * 이미지 삭제 권한 및 중복 검증
     */
    @Transactional(readOnly = true)
    public void validateDeleteImages(Item post, List<Long> deleteImageIds) {
        if (deleteImageIds == null || deleteImageIds.isEmpty()) return;

        if (post.getItemImageList() == null) {
            throw new BusinessException("삭제할 이미지가 존재하지 않습니다.");
        }

        // 게시글에 실제로 연결되어 있는 이미지 ID 추출
        Set<Long> imageIds = post.getItemImageList().stream()
                .map(ItemImage::getImageId)
                .collect(Collectors.toSet());

        // 요청된 ID 중 해당 상품의 이미지가 아닌 것이 있는지 검증
        for (Long deleteId : deleteImageIds) {
            if (!imageIds.contains(deleteId)) {
                throw new BusinessException("해당 상품에 속하지 않은 고유 번호가 삭제 요청에 포함되어 있습니다.");
            }
        }

        // 중복된 ID 요청 필터링 검증
        long distinctDeleteCount = deleteImageIds.stream().distinct().count();
        if (distinctDeleteCount < deleteImageIds.size()) {
            throw new BusinessException("중복된 삭제 이미지 요청 항목이 존재합니다.");
        }
    }
}

