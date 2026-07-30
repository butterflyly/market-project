package com.example.demo.Service.Item;

import com.example.demo.Entity.Item.ItemCategory;
import com.example.demo.Repository.Item.Item.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemCategoryService {

    private final ItemCategoryRepository itemCategoryRepository;

    public List<ItemCategory> findAll() {

        return itemCategoryRepository.findAll();
    }

    public ItemCategory findByCategory(Long categoryId) {
        return itemCategoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다"));
    }

}
