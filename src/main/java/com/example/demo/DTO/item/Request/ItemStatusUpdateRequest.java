package com.example.demo.DTO.item.Request;

import com.example.demo.Entity.Item.ItemStatus;
import jakarta.validation.constraints.NotNull;

public record ItemStatusUpdateRequest(
        @NotNull(message = "변경할 판매 상태는 필수 선택 사항입니다.")
        ItemStatus itemStatus,

        String buyerUsername
) {
}
