package com.example.demo.Service.Item;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemLike;
import com.example.demo.Entity.Users.Users;
import com.example.demo.Error.BusinessException;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import com.example.demo.Service.Users.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemLikeCommandService {

    private final ItemLikeRepository itemLikeRepository;
    private final ItemReadService itemReadService;
    private final UserReadService userReadService;

    public void toggleLike(Long itemId, String username) {

        // 1. 엔티티 조회
        Item item = itemReadService.getItemEntity(itemId);
        Users user = userReadService.getUserEntity(username);

        // 2. 본인 상품 검증
        if (item.getSeller().getUsername().equals(username)) {
            throw new BusinessException("본인이 판매하는 상품은 찜할 수 없습니다.");
        }

        // 3. 기존 찜 여부 확인
        ItemLike itemLike = itemLikeRepository.findByItemAndUsers(item, user);

        // 4. 토글(Toggle) 로직 수행
        if (itemLike != null) {
            // 이미 추천을 한 경우 -> 찜 취소
            itemLikeRepository.delete(itemLike);
            item.decreaseLikeCount();
        } else {
            // 추천을 안 한 경우 -> 찜 등록
            ItemLike newLike = ItemLike.builder()
                    .users(user)
                    .item(item)
                    .build();

            itemLikeRepository.save(newLike);
            item.increaseLikeCount();
        }
    }
}
