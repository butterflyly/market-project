package com.example.demo.Security.Config;


import com.example.demo.Security.Handler.AlreadyLoggedInAccessDeniedHandler;
import com.example.demo.Security.JWT.JwtFilter;
import com.example.demo.Security.OAuth2.OAuth2SuccessHandler;
import com.example.demo.Security.OAuth2.PrincipalOAuth2UserService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final PrincipalOAuth2UserService principalOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final AlreadyLoggedInAccessDeniedHandler alreadyLoggedInAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        //  SSE 스트리밍 및 비동기 요청 보안 통과 허용
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.FORWARD).permitAll()

                        // SSE 구독 및 알림 API 허용
                        .requestMatchers("/api/notifications/**").authenticated()

                        // 1. [공통] 정적 리소스 및 파일 관련
                        .requestMatchers("/favicon.ico", "/css/**",
                                "/js/**", "/images/**",
                                "/itemImages/**",
                                "/profileImages/**",
                                "/commentImages/**",
                                "/chatImages/**"
                        ).permitAll()

                        // 2. [OAuth2 / 소셜 로그인]
                        .requestMatchers(
                                "/login/oauth2/code/**",
                                "/oauth2/authorization/**"
                        ).permitAll()

                        // 3. [유저 도메인]
                        .requestMatchers("/users/register",
                                "/users/login","/users/find-userId","/uers/find-password").anonymous()
                        .requestMatchers("/api/users/login", "/api/users/register/**"
                                ,"/api/users/find-userId" , "/api/users/find-password"
                                ,"/api/users/mail-auth", "/api/users/mail-verify").anonymous()

                        .requestMatchers("/api/users/me").permitAll()

                        .requestMatchers(
                                "/api/users/update",
                                "/api/users/update-password",
                                "/api/users/delete"
                        ).authenticated()

                        .requestMatchers(
                                "/users/mypage",
                                "/users/items/my-list",
                                "/users/mypage/chats",
                                "/users/update",
                                "/users/update/password",
                                "/users/withdraw/confirm"
                        ).authenticated()

                        // 4. [상품 도메인]
                        .requestMatchers(HttpMethod.GET, "/items/list/**", "/items/detail/**").permitAll()
                        .requestMatchers("/items/create", "/items/edit/**").authenticated()
                        .requestMatchers("/api/items/**").authenticated()

                        // 5. 웹소켓
                        .requestMatchers("/ws-stomp/**").permitAll()

                        // 6. [신고 / 어드민]
                        .requestMatchers("/api/reports/**").authenticated()
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                        // 8. [채팅]
                        .requestMatchers("/api/chat-room/**", "/api/chat/**", "/chat/room/**", "/api/notifications/**").authenticated()

                        // 9. [리뷰]
                        .requestMatchers(HttpMethod.GET, "/reviews/seller/**").permitAll()
                        .requestMatchers("/reviews/write", "/reviews/edit/**", "/api/reviews/**").authenticated()

                        .anyRequest().authenticated()

                )

                //  소셜 로그인 핸들러 및 서비스 바인딩 구조 추가
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/users/login") // 비로그인 유저 보호막 작동 시 튕겨나갈 소셜 연동 로그인 페이지
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(principalOAuth2UserService) // 사용자 정보 파싱 서비스 등록
                        )
                        .successHandler(oAuth2SuccessHandler) // 인증 성공 시 쿠키에 JWT 구워줄 성공 핸들러 등록
                )

                //  Stateless JWT 설정 및 필터 체인 배치
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // 에러 핸들링
                .exceptionHandling(exception -> exception
                        // 1. Accept 헤더가 text/event-stream 인 요청은 401 상태코드 직접 반환
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new MediaTypeRequestMatcher(MediaType.TEXT_EVENT_STREAM)
                        )
                        .accessDeniedHandler(alreadyLoggedInAccessDeniedHandler)
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getRequestURI();
                            String acceptHeader = request.getHeader("Accept");
                            String queryString = request.getQueryString();
                            String targetPath = (queryString != null) ? path + "?" + queryString : path;

                            // API 요청 또는 SSE 요청인 경우 HTML 리다이렉트 대신 401 JSON/Error 반환
                            if (path.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains("text/event-stream"))) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"success\":false,\"message\":\"인증이 필요합니다.\"}");
                            } else {
                                response.sendRedirect("/users/login?redirectUrl=" + URLEncoder.encode(targetPath, StandardCharsets.UTF_8));
                            }
                        })
                );

        return http.build();
    }


    /*
    기존 Spring Security(버전 5 이하)는 내부적으로 AuthenticationManager 를 자동으로 생성해줬음
    하지만 Spring Security 6에서는:

    "개발자가 어떤 인증 방식을 쓰는지 모르니
    AuthenticationManager는 직접 설정해라."
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}