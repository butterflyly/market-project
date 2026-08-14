package com.example.demo.Service.Item;

import com.example.demo.DTO.Users.Response.MyPageItemResponse;
import com.example.demo.DTO.item.Response.ItemSummaryResponse;
import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Repository.Item.ItemLike.ItemLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemLikeReadService {

    private final ItemLikeRepository itemLikeRepository;

    public Page<MyPageItemResponse> getLikedItemsByUsername(String username, Pageable pageable) {
        return itemLikeRepository.findByMyLikePage(username, pageable)
                .map(itemLike -> {
                    Item item = itemLike.getItem();
                    return MyPageItemResponse.of(item);
                });
    }

    public Page<ItemSummaryResponse> getLikedItemsByUsernameList(String username, Pageable pageable) {
        return itemLikeRepository.findByMyLikePage(username, pageable)
                .map(itemLike -> {
                    Item item = itemLike.getItem();
                    return ItemSummaryResponse.from(item);
                });
    }
}