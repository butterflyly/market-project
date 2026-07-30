package com.example.demo.DTO;

// 모든 API의 표준 응답 규격
public record CommonResponse<T>(
        boolean isAvailable,
        String message,
        T data // 로그인 정보나 중복 여부 등 추가 데이터가 필요할 때 사용
) {}