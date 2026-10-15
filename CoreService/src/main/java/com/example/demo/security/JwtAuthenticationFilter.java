package com.example.demo.security;

import io.jsonwebtoken.ExpiredJwtException;
import com.example.demo.exception.ApiErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklistService blacklistService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // WebSocket 요청인지 확인하고 필터링 제외
        String upgradeHeader = request.getHeader("Upgrade");
        String path = request.getRequestURI();

        return upgradeHeader != null && "websocket".equalsIgnoreCase(upgradeHeader)
                && ("/ws".equals(path) || path.startsWith("/ws/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");
        String token = resolveToken(request);
        String path = request.getRequestURI();

        if (invalidPath(path)) {
            JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.INVALID_PATH);
            return;
        }

        // 로깅 추가
        log.debug("Received request to path: {}", path);

        if (StringUtils.hasText(authorization) && (token == null || token.isBlank())) {
            JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.AUTH_INVALID);
            return;
        }

        if (token != null) {
            try {
                boolean isValid = jwtTokenProvider.validateToken(token);
                if (!isValid) {
                    try {
                        jwtTokenProvider.getExpirationDate(token);
                    } catch (ExpiredJwtException e) {
                        JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.AUTH_EXPIRED);
                        return;
                    } catch (Exception e) {
                        // 위조·손상된 토큰은 같은 코드로 응답한다.
                    }
                    JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.AUTH_INVALID);
                    return;
                }
                if (!blacklistService.isBlacklisted(token)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authentication successful for user: {}", auth.getName());
                } else {
                    JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.AUTH_INVALID);
                    log.warn("Attempt to use blacklisted token for path: {}", path);
                    return;
                }
            } catch (Exception e) {
                log.warn("JWT Authentication Error for path: {}", path);
                JwtAuthenticationEntryPoint.writeError(response, ApiErrorCode.AUTH_INVALID);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean invalidPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return path.contains("//") || path.indexOf('\\') >= 0 || path.endsWith("/")
                || path.equals("/.") || path.equals("/..") || path.contains("/./") || path.contains("/../")
                || path.endsWith("/.") || path.endsWith("/..")
                || lower.matches("(?s).*%(?:2f|5c|00|2e|252f|255c|2500|252e).*");
    }

    /**
     * HTTP 요청 헤더에서 JWT 토큰을 추출합니다.
     * "Authorization: Bearer [token]" 형식에서 토큰 부분만 반환합니다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
