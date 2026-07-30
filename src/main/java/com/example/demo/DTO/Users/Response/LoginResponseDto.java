package com.example.demo.DTO.Users.Response;

import org.springframework.http.ResponseCookie;

public record LoginResponseDto(
        ResponseCookie accessCookie,
        ResponseCookie refreshCookie,
        String nickname,
        String profileUrl
) {
}
