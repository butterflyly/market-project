package com.example.demo.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Value("${file.profile.url-path}") private String profileUrl;
    @Value("${file.profile.absolute-path}") private String profileLocation;

    @Value("${file.item.url-path}") private String itemUrl;
    @Value("${file.item.absolute-path}") private String itemLocation;

    @Value("${file.comment.url-path}") private String commentUrl;
    @Value("${file.comment.absolute-path}") private String commentLocation;

    @Value("${file.delete-item.url-path}") private String deleteItemUrl;
    @Value("${file.delete-item.absolute-path}") private String deleteItemLocation;

    @Value("${file.chat.url-path}") private String chatUrl;
    @Value("${file.chat.absolute-path}") private String chatLocation;


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 리소스 등록 및 핸들러를 관리하는 객체인 ResourceHandlerRegistry를 통해 리소스의 위치와 리소스와 매칭 될 url을 설정
        // 프로필 이미지
        registry.addResourceHandler(profileUrl) // 사용자의 요청을 Resource로 맵핑할 경로를 적어주면 됨
                .addResourceLocations(profileLocation);    // 실제 리소스가 존재하는 외부 경로를 지정

        // 댓글 이미지
        registry.addResourceHandler(commentUrl)
                .addResourceLocations(commentLocation);

        // 아이템(상품) 이미지
        registry.addResourceHandler(itemUrl)
                .addResourceLocations(itemLocation);

        // 삭제된 아이템 이미지
        registry.addResourceHandler(deleteItemUrl)
                .addResourceLocations(deleteItemLocation);

        registry.addResourceHandler(chatUrl)
                .addResourceLocations(chatLocation);
    }
}
