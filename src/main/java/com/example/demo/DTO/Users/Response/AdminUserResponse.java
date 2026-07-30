package com.example.demo.DTO.Users.Response;

import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;

import java.time.LocalDateTime;

/**
 * 유저 관리 목록용 (블랙 유저 혹은 소프트 유저 리스트에 공통 사용)
 */
public record AdminUserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        UserRole role,
        int warningCount,
        boolean deleted, // true면 소프트 유저(탈퇴)
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(Users user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getRole(),
                user.getWarningCount(),
                user.isDeleted(),
                user.getCreatedAt()
        );
    }
}