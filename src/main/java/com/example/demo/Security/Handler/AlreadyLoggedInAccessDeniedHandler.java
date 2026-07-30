package com.example.demo.Security.Handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class AlreadyLoggedInAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 이전 주소 가져오기
        String referer = request.getHeader("Referer");
        String requestURI = request.getRequestURI();

        log.info("이미 로그인한 유저가 비인족 페이지 접근 시도 -> 요청 URI: {}, 이전 페이지: {}", requestURI, referer);

        // 이전 페이지가 존재하고, 무한 루프 방지를 위해 현재 요청한 페이지와 다르다면 거기로 튕겨내기
        if (referer != null && !referer.contains(requestURI)) {
            response.sendRedirect(referer);
            return;
        }

        // 만약 주소창에 직접 타이핑해서 이전 페이지 정보(Referer)가 없다면 메인이나 상품 목록으로 이동
        response.sendRedirect("/items/list/1");
    }
}
