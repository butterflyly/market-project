package com.example.demo.DTO.Users.Request.Auth;

import jakarta.validation.constraints.NotBlank;


public record LoginRequest(

    @NotBlank
    String username,

    @NotBlank
    String password,
    boolean rememberMe // 자동로그인
) {
}
