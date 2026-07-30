package com.example.demo.DTO.Alram;

import com.example.demo.Entity.Users.Users;

public record UserWarningEvent(
        Users warnedUser, // 경고받은 유저
        int warningCount, // 부여 후 총 누적 경고 횟수
        boolean isBlackened // 이번 경고로 블랙리스트 전환 여부
) {}