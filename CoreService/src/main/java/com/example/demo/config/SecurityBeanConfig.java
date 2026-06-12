package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.demo.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.demo.security.oauth2.OAuth2AuthenticationSuccessHandler;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class SecurityBeanConfig {

    @Value("${oauth2.authorizedRedirectUris}")
    private String[] authorizedRedirectUris;

    /**
     * OAuth2 인증 요청을 쿠키에 저장하는 리포지토리
     */
    @Bean
    @Primary
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        log.info("HttpCookieOAuth2AuthorizationRequestRepository 빈 초기화");
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    /**
     * OAuth2 인증 성공 시 처리하는 핸들러
     */
    @Bean
    @Primary
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider, 
            HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        
        List<String> authorizedUris = Arrays.asList(authorizedRedirectUris);
        log.info("OAuth2AuthenticationSuccessHandler 빈 초기화 - 허용된 리다이렉트 URI: {}", authorizedUris);
        return new OAuth2AuthenticationSuccessHandler(jwtTokenProvider, authorizedUris, cookieAuthorizationRequestRepository);
    }

    /**
     * OAuth2 인증 실패 시 처리하는 핸들러
     */
    @Bean
    @Primary
    public OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler(
            HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        
        log.info("OAuth2AuthenticationFailureHandler 빈 초기화");
        return new OAuth2AuthenticationFailureHandler(cookieAuthorizationRequestRepository);
    }
}
