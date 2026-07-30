package com.example.demo.DTO.item.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record ItemUpdateRequest (
        @NotBlank(message = "제목은 필수 입력사항입니다.")
        @Size(min = 2, message = "제목은 최소 2글자 이상이어야 합니다.")
        String title,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price,

        @NotBlank(message = "내용은 필수 입력사항입니다.")
        String content,

        @NotNull(message = "카테고리를 선택해주세요.")
        Long categoryId,

        List<MultipartFile> images
) {

}