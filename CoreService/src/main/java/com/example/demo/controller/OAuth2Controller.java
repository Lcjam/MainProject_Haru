package com.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import com.example.demo.dto.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/core/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    /**
     * 구글 로그인 페이지로 리다이렉트
     */
    @GetMapping("/google")
    public RedirectView googleLogin() {
        log.info("구글 로그인 요청");
        return new RedirectView("/api/core/auth/oauth2/authorize/google?redirect_uri=http://localhost:3000/oauth2/redirect");
    }

    /**
     * 네이버 로그인 페이지로 리다이렉트
     */
    @GetMapping("/naver")
    public RedirectView naverLogin() {
        log.info("네이버 로그인 요청");
        return new RedirectView("/api/core/auth/oauth2/authorize/naver?redirect_uri=http://localhost:3000/oauth2/redirect");
    }

    /**
     * 카카오 로그인 페이지로 리다이렉트
     */
    @GetMapping("/kakao")
    public RedirectView kakaoLogin() {
        log.info("카카오 로그인 요청");
        return new RedirectView("/api/core/auth/oauth2/authorize/kakao?redirect_uri=http://localhost:3000/oauth2/redirect");
    }

    /**
     * OAuth2 로그인 상태 체크
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "OAuth2 인증 서비스가 활성화되어 있습니다.");
        response.put("providers", new String[]{"google", "naver", "kakao"});
        
        return ApiResponse.success(response);
    }
}
