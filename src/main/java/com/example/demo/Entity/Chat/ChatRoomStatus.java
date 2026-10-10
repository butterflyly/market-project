package com.example.demo.Entity.Chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChatRoomStatus {

    BOTH_IN("모두 참여 중"),
    HOST_OUT("판매자 퇴장"),
    GUEST_OUT("구매자 퇴장"),
    BOTH_OUT("모두 퇴장");

    private final String description;
}