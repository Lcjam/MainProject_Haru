package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import com.example.demo.security.oauth2.CustomOAuth2UserService;
import com.example.demo.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.demo.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.example.demo.security.oauth2.OAuth2AuthenticationSuccessHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class OAuth2Config {
    
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    
    // Bean 선언 부분 제거 - SecurityBeanConfig로 이동

    /**
     * HttpSecurity에 OAuth2 로그인 설정을 구성합니다.
     * SecurityConfig에서 호출됩니다.
     */
    public void configure(HttpSecurity http) throws Exception {
        log.debug("OAuth2 설정 적용 - 인증 엔드포인트: /api/core/auth/oauth2/authorize");
        
        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                        .baseUri("/api/core/auth/oauth2/authorize")
                        .authorizationRequestRepository(authorizationRequestRepository)
                )
                .redirectionEndpoint(endpoint -> endpoint
                        .baseUri("/api/core/auth/oauth2/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
        );
    }
}
