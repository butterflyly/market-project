package com.example.demo;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemImage;
import com.example.demo.Entity.Item.ItemStatus;
import com.example.demo.Entity.Users.Users;

import java.util.List;

public class TestFixture {

    public static Users createMockUsers(String username) {
        return createMockUsers(username, 0);
    }

    public static Users createMockUsersTotal(String username) {
        return Users.builder()
                .username(username )
                .nickname("테스트유저" )
                .email("테스트이메일@naver.com")
                .password("encodedPassword")
                .build();
    }

    public static Users createMockUsersTotal2(String username) {
        return Users.builder()
                .username(username )
                .nickname("테스트유저2" )
                .email("테스트이메일2@naver.com")
                .password("encodedPassword")
                .build();
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



    public static Item createMockItem(Long id, Users seller, List<ItemImage> itemImages)
    {
        Item item = Item.builder()
                .id(id)
                .title("테스트 상품 제목")
                .content("테스트 상품 내용")
                .price(100000)
                .seller(seller)
                .itemStatus(ItemStatus.ON_SALE)
                .deleted(false)
                .build();

        // 이미지가 존재한다면 아이템과 연관관계 매핑
        if (itemImages != null && !itemImages.isEmpty()) {
            for (ItemImage image : itemImages) {
                // ItemImage 내부에 연관관계를 맺어주는 메서드가 있거나,
                // 혹은 Item 엔티티 쪽에 편의 메서드가 있다면 활용
                // 예: image.setItem(item);
                item.getItemImageList().add(image);
            }
        }

        return item;
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

    public static Item createMockItemTotal(Users seller,Users buyer) {
        return Item.builder()
                .title("테스트 상품 제목")
                .content("테스트 상품 내용")
                .price(100000)
                .seller(seller)
                .buyer(buyer)
                .itemStatus(ItemStatus.ON_SALE)
                .deleted(false)
                .build();
    }

    public static Item createMockItem(Long id, Users seller,Users buyer ) {
        return Item.builder()
                .id(id)
                .title("테스트 상품 제목")
                .content("테스트 상품 내용")
                .price(100000)
                .seller(seller)
                .buyer(buyer)
                .itemStatus(ItemStatus.SOLD_OUT)
                .deleted(false)
                .build();
    }
}