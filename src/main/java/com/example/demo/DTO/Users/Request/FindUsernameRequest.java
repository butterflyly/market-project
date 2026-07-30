package com.example.demo.DTO.Users.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record FindUsernameRequest(
        @NotBlank(message = "이메일은 필수 입력값입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email
) {}