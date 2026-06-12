package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 프로필 이미지를 접근할 수 있도록 리소스 핸들러 등록
        registry.addResourceHandler("/profile-images/**")
                .addResourceLocations("classpath:/static/profile-images/");
        
        // 채팅 이미지를 접근할 수 있도록 리소스 핸들러 설정
        registry.addResourceHandler("/chat-images/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/uploads/chat-images/");
        
        // 게시판 파일을 접근할 수 있도록 리소스 핸들러 설정
        registry.addResourceHandler("/board-files/**")
                .addResourceLocations("classpath:/static/board-files/");
        
        // 기존 업로드 디렉토리 설정 유지
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/src/main/resources/static/uploads/");
    }  
}
