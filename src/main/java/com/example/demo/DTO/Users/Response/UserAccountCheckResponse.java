package com.example.demo.DTO.Users.Response;

import com.example.demo.Entity.Users.Users;

public record UserAccountCheckResponse(
        String username,
        String email,
        String providers
) {
    public static UserAccountCheckResponse of(Users user) {
        return new UserAccountCheckResponse(
                user.getUsername(),
                user.getEmail(),
                user.getProviders()
        );
    }

    // 소셜 유저 여부를 판단하는 편의 메서드
    public boolean isSocialUser() {
        return providers != null && !providers.isBlank();
    }
}