package com.example.demo.security.oauth2;

import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class OAuth2LoginCodeService {
    private static final String EXCHANGE_ATTRIBUTE = OAuth2LoginCodeService.class.getName() + ".exchange";
    private static final Duration CODE_TTL = Duration.ofMinutes(2);
    private final SecureRandom secureRandom = new SecureRandom();
    private final JwtTokenProvider jwtTokenProvider;

    public OAuth2LoginCodeService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String issue(HttpServletRequest request, CustomOAuth2User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        LoginResponse login = LoginResponse.builder()
                .success(true)
                .token(jwtTokenProvider.createToken(user.getUserId(), user.getEmail(), roles))
                .email(user.getEmail())
                .nickname(user.getNickname())
                .message("로그인 성공")
                .build();
        HttpSession session = request.getSession();
        synchronized (session) {
            session.setAttribute(EXCHANGE_ATTRIBUTE, new PendingExchange(code, login, Instant.now()));
        }
        return code;
    }

    public LoginResponse consume(HttpServletRequest request, String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        synchronized (session) {
            PendingExchange pending = (PendingExchange) session.getAttribute(EXCHANGE_ATTRIBUTE);
            if (pending == null || pending.createdAt().plus(CODE_TTL).isBefore(Instant.now())) {
                session.removeAttribute(EXCHANGE_ATTRIBUTE);
                return null;
            }
            if (!MessageDigest.isEqual(pending.code().getBytes(StandardCharsets.US_ASCII),
                    code.getBytes(StandardCharsets.US_ASCII))) {
                return null;
            }
            session.removeAttribute(EXCHANGE_ATTRIBUTE);
            return pending.login();
        }
    }

    private record PendingExchange(String code, LoginResponse login, Instant createdAt) { }
}
