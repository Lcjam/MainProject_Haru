package com.example.demo.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;

/**
 * 토큰 관련 유틸리티 클래스
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenUtils {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;

    /**
     * 토큰에서 Bearer 접두사를 제거합니다
     */
    public String extractTokenWithoutBearer(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    /**
     * 토큰이 유효하고 블랙리스트에 없는지 확인합니다
     */
    public boolean isTokenValid(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        return jwtTokenProvider.validateToken(token) && !jwtTokenBlacklistService.isBlacklisted(token);
    }

    /**
     * 토큰에서 사용자 ID를 추출합니다
     */
    public Integer getUserIdFromToken(String token) {
        if (!isTokenValid(token)) {
            return null;
        }

        return jwtTokenProvider.getUserId(token);
    }
    
    /**
     * 토큰에서 사용자 이메일을 추출합니다
     */
    public String getEmailFromToken(String token) {
        if (!isTokenValid(token)) {
            return null;
        }
        
        return jwtTokenProvider.getUsername(token);
    }

    /**
     * Authorization 헤더에서 이메일 정보를 추출합니다.
     * 토큰이 유효하지 않은 경우 null을 반환합니다.
     * @param authHeader Authorization 헤더 값 (Bearer 토큰 포함)
     * @return 사용자 이메일 또는 null (토큰이 유효하지 않은 경우)
     */
    public String getEmailFromAuthHeader(String authHeader) {
        String token = extractTokenWithoutBearer(authHeader);
        
        if (!isTokenValid(token)) {
            return null;
        }
        
        return getEmailFromToken(token);
    }
}