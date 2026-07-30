package com.example.demo.DTO.Alram;

import com.example.demo.Entity.Item.Item;
import com.example.demo.Entity.Item.ItemStatus;

public record ItemStatusChangedEvent(
        Item item,
        String targetUrl

) {
}
