package com.example.demo.DTO.Alram;

import com.example.demo.Entity.Users.Users;

public record DealConfirmedEvent(

        Users buyer,
        String sellerNickname,
        String title,
        String targetUrl
) {
}
