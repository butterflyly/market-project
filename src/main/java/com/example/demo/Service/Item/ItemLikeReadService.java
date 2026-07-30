package com.example.demo.Service.Item;

import com.example.demo.DTO.item.Response.ItemSummaryResponse;
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

    public Page<ItemSummaryResponse> getLikedItemsByUsername(String username, Pageable pageable) {
        return itemLikeRepository.findByMyLikePage(username, pageable)
                .map(itemLike -> ItemSummaryResponse.from(itemLike.getItem()));
    }
}