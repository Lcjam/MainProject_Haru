package com.example.demo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${security.jwt.token.secret-key}") String secretKeyString,
            @Value("${security.jwt.token.expire-length}") long validityInMilliseconds) {

        String base64EncodedSecretKey = Base64.getEncoder().encodeToString(secretKeyString.getBytes(StandardCharsets.UTF_8));
        this.secretKey = Keys.hmacShaKeyFor(base64EncodedSecretKey.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String createToken(Integer userId, String email, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(email);
        claims.put("userId", userId);
        claims.put("roles", roles);

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds); // 일반적으로 24시간

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody();

        // "roles" claim을 올바른 타입으로 가져오도록 수정
        List<SimpleGrantedAuthority> authorities;

        // roles가 ArrayList인 경우 처리
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) rolesObj;
            authorities = roles.stream()
                 .map(SimpleGrantedAuthority::new)
                 .collect(Collectors.toList());

        } else if (rolesObj instanceof String) {
            // 이전 방식과의 호환성을 위해 String인 경우도 처리
            String rolesStr = (String) rolesObj;
            authorities = Arrays.stream(rolesStr.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        } else {
            // 역할이 없거나 알 수 없는 형식인 경우 빈 목록 사용
            authorities = Collections.emptyList();
        }
        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

//    public String getUsername(String token) {
//        return Jwts.parserBuilder().setSigningKey(secretKey).build()
//                .parseClaimsJws(token).getBody().getSubject();
//    }

    public String getUsername(String token) {
        // "Bearer " 제거 후 순수 토큰만 추출
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);  // "Bearer " 부분 제거
        }

        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
  
    public Integer getUserId(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build()
                .parseClaimsJws(token).getBody().get("userId", Integer.class);
    }

    public boolean validateToken(String token) {
        try {
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(secretKey).build()
                    .parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰의 만료 시간을 가져오는 메소드
     */
    public Date getExpirationDate(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

}

