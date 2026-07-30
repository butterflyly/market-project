package com.example.demo.DTO.Users.Response;

import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;

import java.time.LocalDateTime;

public record AdminUserDetailResponse(
        Long id,
        String username,
        String nickname,
        String displayNickname,
        String email,
        UserRole role,
        int warningCount,
        boolean deleted,
        LocalDateTime createdAt,
        String profileUrl,
        boolean isPermanentPreserved
) {
    public static AdminUserDetailResponse from(Users user) {
        return new AdminUserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getDisplayNickname(),
                user.getEmail(),
                user.getRole(),
                user.getWarningCount(),
                user.isDeleted(),
                user.getCreatedAt(),
                user.getProfileUrl(),
                user.isPermanentPreserved()
        );
    }
}