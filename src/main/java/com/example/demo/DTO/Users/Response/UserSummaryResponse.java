package com.example.demo.DTO.Users.Response;

import com.example.demo.Entity.Users.Users;

public record UserSummaryResponse (

    String username,
    String nickname,
    int warningCount
)
{
    public static UserSummaryResponse of(Users users)
    {
        return new UserSummaryResponse(
                users.getUsername(),
                users.getNickname(),
                users.getWarningCount());
    }
}