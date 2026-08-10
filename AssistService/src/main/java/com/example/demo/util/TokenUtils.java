package com.example.demo.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
@Slf4j
public class TokenUtils {

    private final SecretKey secretKey;

    public TokenUtils(@Value("${jwt.secret}") String secret) {
        this.secretKey = deriveKey(secret);
    }

    /**
     * HMAC 키 파생.
     *
     * <p><b>CoreService {@code JwtTokenProvider} · GateWay {@code JwtAuthenticationFilter} 와
     * 반드시 동일해야 한다.</b> 두 서비스는 시크릿을 Base64 로 인코딩한 <i>문자열의 바이트</i>로
     * 키를 만든다(이중 인코딩 — 비표준이지만 이미 발급된 토큰의 서명 기준이다).
     * 여기서 표준 파생({@code secret.getBytes()})을 쓰면 같은 시크릿인데도 모든 토큰이
     * 검증 실패한다. {@code TokenUtilsTest} 의 교차 검증 테스트가 이 불일치를 잡는다.
     */
    private static SecretKey deriveKey(String secret) {
        String base64EncodedSecretKey =
                Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(base64EncodedSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    // "Bearer " 접두사만 제거 (전역 replace 아님)
    public String extractTokenWithoutBearer(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    /**
     * JWT 서명을 검증한 뒤 {@code sub}(이메일)를 반환한다.
     * 서명 불일치·만료·형식 오류는 모두 {@code null} 이다.
     *
     * <p>이전 구현은 서명 검증 없이 페이로드만 Base64 디코딩해 {@code sub} 를 읽었다(S2).
     * 아무나 만든 토큰으로 임의 사용자를 사칭할 수 있었다.
     */
    public String getEmailFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(extractTokenWithoutBearer(token))
                    .getBody();

            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("토큰 검증 실패: {}", e.getMessage());
            return null;
        }
    }
}
