package com.example.demo.DTO.Chat.Response;

import com.example.demo.Entity.Users.Users;

public record ChatBuyerResponse(
        String username,        // API 요청에 실어 보낼 고유 아이디
        String displayNickname, // 화면에 보여줄 보조 닉네임
        String profileUrl       // 팝업 목록에 함께 띄워줄 프로필 이미지 경로
) {
    // Users 엔티티를 간편하게 DTO로 변환하는 static factory 메서드
    public static ChatBuyerResponse from(Users user) {
        return new ChatBuyerResponse(
                user.getUsername(),
                user.getDisplayNickname(),
                user.getProfileUrl() != null ? user.getProfileUrl() : "/images/default-profile.png"
        );
    }
}