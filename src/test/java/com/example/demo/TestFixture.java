package com.example.demo;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;

public class TestFixture {

    public static Users createMockUsers(String username) {
        return createMockUsers(username, 0);
    }

    public static Users createMockUsers(String username, int warningCount) {
        Users users = Users.builder()
                .id(1L)
                .username(username)
                .build();

        for (int i = 0; i < warningCount; i++) {
            users.receiveWarning();
        }
        return users;
    }

    public static Item createMockItem(Long id, Users seller) {
        return createMockItem(id, seller, ItemStatus.ON_SALE);
    }

    public static Item createMockItem(Long id, Users seller, ItemStatus status) {
        return Item.builder()
                .id(id)
                .title("테스트 상품 제목")
                .content("테스트 상품 내용")
                .price(100000)
                .seller(seller)
                .itemStatus(status)
                .deleted(false)
                .build();
    }
}