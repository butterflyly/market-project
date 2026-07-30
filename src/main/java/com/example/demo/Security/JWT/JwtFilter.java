package com.example.demo.Security.JWT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = resolveToken(request, "accessToken");
        String refreshToken = resolveToken(request, "refreshToken");

        // 1. Access Token이 존재하고 유효한 경우 -> 정상 인증 진행
        if (accessToken != null && jwtProvider.validateToken(accessToken)) {
            setAuthenticationToContext(accessToken);
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Access Token이 만료되었거나 없는데, Refresh Token은 존재하는 경우
        if (refreshToken != null && jwtProvider.validateToken(refreshToken)) {
            String username = jwtProvider.getUsername(refreshToken);

            // Redis에 저장된 Refresh Token과 일치하는지 검증
            String savedRefreshToken = (String) redisTemplate.opsForValue().get(username);

            if (savedRefreshToken != null && savedRefreshToken.equals(refreshToken)) {
                log.info("Access Token 만료됨. Refresh Token 기반으로 인증 및 Access Token 재발급 진행: {}", username);

                // DB에서 UserDetails 정보 조회 후 새로운 Authentication 생성
                Authentication newAuth = jwtProvider.getAuthenticationFromUsername(username);

                // 새로운 Access Token 생성
                // (기존 rememberMe 쿠키의 maxAge가 -1인지 확인하여 rememberMe 상태 판별)
                boolean isRememberMe = isRememberMeCookie(request);
                JwtToken newToken = jwtProvider.createToken(newAuth, isRememberMe);

                // 새 AccessToken 쿠키 생성 후 응답 헤더에 세팅
                ResponseCookie newAccessCookie = jwtProvider.createAccessCookie(newToken.getAccessToken());
                response.addHeader("Set-Cookie", newAccessCookie.toString());

                // 시큐리티 컨텍스트에 인증 정보 등록
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            } else {
                log.warn("Redis의 Refresh Token과 일치하지 않음 또는 탈취 위험: {}", username);
                SecurityContextHolder.clearContext();
            }
        } else {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthenticationToContext(String token) {
        Authentication authentication = jwtProvider.getAuthentication(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.info("JWT 인증 성공: {}", authentication.getName());
    }

    // 요청에서 지정한 이름의 쿠키 값을 추출하는 헬퍼 메서드
    private String resolveToken(HttpServletRequest request, String cookieName) {
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

    // Refresh Token 쿠키의 만료시간 설정 여부로 rememberMe 파악
    private boolean isRememberMeCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return false;
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                // maxAge가 0보다 크면 영속 쿠키(rememberMe=true)로 판단
                return cookie.getMaxAge() > 0;
            }
        }
        return false;
    }
}