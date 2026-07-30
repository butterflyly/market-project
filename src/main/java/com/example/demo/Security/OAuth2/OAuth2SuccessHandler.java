package com.example.demo.Security.OAuth2;


import com.example.demo.Repository.Users.UserRepository;
import com.example.demo.Security.JWT.JwtProvider;
import com.example.demo.Security.JWT.JwtToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        // 1. JWT 토큰 생성 및 쿠키 설정
        JwtToken jwtToken = jwtProvider.createToken(authentication, false);

        //  이미 만들어둔 JwtProvider의 안전한 쿠키 조립기 활용!
        ResponseCookie accessCookie = jwtProvider.createAccessCookie(jwtToken.getAccessToken());
        ResponseCookie refreshCookie = jwtProvider.createRefreshCookie(jwtToken.getRefreshToken(), false);

        log.info("OAuth2 로그인 성공 - username: {}, issuing tokens", authentication.getName());

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // 2. PrincipalDetails에서 isNewUser 여부 추출
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        boolean isNewUser = principalDetails.isNewUser();

        String targetUrl;

        if (isNewUser) {
            // 최초 가입자는 프로필 설정이나 회원정보 완성을 위해 마이페이지(또는 회원가입 완료 페이지)로 보냄
            targetUrl = "/users/mypage";
        } else {
            //  [핵심] 쿠키에 저장해둔 redirectUrl 파라미터 읽기
            String redirectUrl = getCookieValue(request, "redirectUrl");

            if (redirectUrl != null && !redirectUrl.isBlank() && !redirectUrl.contains("/users/login")) {
                targetUrl = redirectUrl;
                // 사용한 쿠키 즉시 만료(삭제)
                deleteCookie(response, "redirectUrl");
            } else {
                targetUrl = "/items/list/1"; // 기본 이동 주소
            }
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    // ----------------------------------------------------
    // 쿠키 조회 및 삭제 헬퍼 메서드
    // ----------------------------------------------------
    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(name)) {
                    return java.net.URLDecoder.decode(cookie.getValue(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
        }
        return null;
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        ResponseCookie cookie = ResponseCookie.from(name, "")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }
}

