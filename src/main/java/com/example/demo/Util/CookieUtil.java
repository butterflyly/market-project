package com.example.demo.Util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component //  빈으로 등록하여 어디서든 주입받을 수 있게 합니다.
public class CookieUtil {

    // 쿠키 값 조회
    public String getCookieValue(HttpServletRequest request, String cookieName) {

        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    // 토큰 쿠키 초기화 (삭제)
    public void clearTokenCookies(HttpServletResponse response) {
        ResponseCookie clearAccessCookie = createClearCookie("accessToken");
        ResponseCookie clearRefreshCookie = createClearCookie("refreshToken");

        response.addHeader("Set-Cookie", clearAccessCookie.toString());
        response.addHeader("Set-Cookie", clearRefreshCookie.toString());
    }

    // 가독성을 위한 내부 쿠키 생성 유틸
    private ResponseCookie createClearCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(0) //  만료시간을 0으로 주어 브라우저가 즉시 삭제하게 만듦
                .build();
    }
}