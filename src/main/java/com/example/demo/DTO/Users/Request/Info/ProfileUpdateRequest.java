package com.example.demo.DTO.Users.Request.Info;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

// 프로필 수정용 (닉네임 + 이미지 대비)
public record ProfileUpdateRequest(
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname,
        MultipartFile profileImage
) {}