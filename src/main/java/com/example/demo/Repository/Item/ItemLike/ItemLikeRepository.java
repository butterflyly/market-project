package com.example.demo.Repository.Item.ItemLike;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemLike;
import com.example.demo.Entity.Users.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemLikeRepository extends JpaRepository<ItemLike, Long> , ItemLikeRepositoryCustom {

    ItemLike findByItemAndUsers(Item item, Users users);

    List<ItemLike> findByItem(Item item);

    boolean existsByItemAndUsersUsername(Item item, String currentUsername);
}
