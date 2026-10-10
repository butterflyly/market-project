package com.example.demo.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminUriInterceptor implements HandlerInterceptor, WebMvcConfigurer {

    // 컨트롤러 실행 후, 뷰 렌더링 직전 동작
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {

        // RestAPI @ResponseBody나 @RestController 요청이 아니라 HTML 화면을 응답하는 요청인 경우
        if (modelAndView != null) {
            modelAndView.addObject("currentUri", request.getRequestURI());
        }
    }

    // 관리자 페이지에서만 로직
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(this).addPathPatterns("/admin/**");
    }
}