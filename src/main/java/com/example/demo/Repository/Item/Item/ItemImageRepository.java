package com.example.demo.Repository.Item.Item;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemImageRepository extends JpaRepository<ItemImage , Long> {

    List<ItemImage> findByImageIdInAndItem(List<Long> deleteImageIds, Item item);
}
