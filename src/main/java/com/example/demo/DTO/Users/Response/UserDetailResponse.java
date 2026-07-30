package com.example.demo.DTO.Users.Response;

import com.example.demo.Entity.Users.UserRole;
import com.example.demo.Entity.Users.Users;
import lombok.Getter;

import java.time.LocalDateTime;


public record UserDetailResponse (
        Long id,
        String username,
        String email,
        UserRole userRole,
        String nickname,
        String displayNickname,
        String profileUrl,
        LocalDateTime createdAt,
        String providers,
        String providerIds
){

    // 아직 생성되지도 않은 DTO 객체에게 일을 시킬 수 없으니,
    // 클래스 틀 자체에 일을 시켜서 새 객체를 리턴받기 위해 사용한 것
    public static UserDetailResponse of(Users users)
    {
        return new UserDetailResponse(
                users.getId(),
                users.getUsername(),
                users.getEmail(),
                users.getRole(),
                users.getNickname(),
                users.getDisplayNickname(),
                users.getProfileUrl(),
                users.getCreatedAt(),
                users.getProviders(),
                users.getProviderIds());
    }
}
