package com.example.demo.DTO.Users.Request.Auth.Register;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record UsernameCheckRequest (
        @NotBlank(message = "아이디는 필수 입력값입니다")
        @Pattern(regexp = "^[a-z0-9A-Z]{6,15}$", message = "아이디는 영어와 숫자만 사용하여 6~15자리여야 합니다.")
        String username
)
{

}
