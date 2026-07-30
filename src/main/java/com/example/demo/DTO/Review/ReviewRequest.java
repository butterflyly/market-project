package com.example.demo.DTO.Review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
        @Min(value = 1, message = "최소 1점 이상이어야 합니다.")
        @Max(value = 5, message = "최대 5점까지 가능합니다.")
        int rating,

        @NotBlank(message = "후기 내용은 비워둘 수 없습니다.")
        String content
) {}