package com.example.demo.Controller;

import com.example.demo.DTO.Users.Response.UserSummaryResponse;
import com.example.demo.Service.Users.UserReadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "com.example.demo.Controller") // 뷰 컨트롤러가 모여있는 패키지 지정
@RequiredArgsConstructor
public class GlobalLayoutControllerAdvice {

    private final UserReadService userReadService;

    @ModelAttribute("user")
    public UserSummaryResponse populateUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return null; // 비로그인 사용자인 경우
        }
        // 로그인한 유저의 DTO 반환 (warningCount, nickname 등 포함)
        return userReadService.getUserSummary(userDetails.getUsername());
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        // 파라미터가 있다면 뒤에 붙여서 넘겨줌 (예: ?message=expired 등 유지용)
        if (queryString != null) {
            return uri + "?" + queryString;
        }
        return uri;
    }
}