package com.example.demo.Entity.Users;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    Black("B" , "블랙 회원"),
    NORMAL("N", "일반 회원"),
    ADMIN("A", "관리자");

    private final String code;
    private final String description;

    /**
     * 블랙리스트(제재) 회원 여부 확인
     * [서비스 제재 정책]
     * - 1회차 경고: NORMAL 상태 유지 (구두/인앱 경고 알림만 발송)
     * - 2회차 이상 경고: BLACK 상태로 전환 (주요 작성/거래 기능 제한)
     */
    public boolean isBlack() {
        return this == Black;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }
}
