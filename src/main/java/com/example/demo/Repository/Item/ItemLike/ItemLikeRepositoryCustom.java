package com.example.demo.Repository.Item.ItemLike;

import com.example.demo.Entity.Item.ItemLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemLikeRepositoryCustom {

    Page<ItemLike> findByMyLikePage(String username, Pageable pageable);

}
