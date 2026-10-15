package com.example.demo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.exception.ApiErrorCode;
import com.example.demo.exception.GatewayErrorHandler;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final ObjectMapper objectMapper;

    private static final Set<String> PUBLIC_ROUTES = Set.of(
            "POST /api/core/auth/signup", "POST /api/core/auth/login",
            "GET /api/core/auth/oauth2/google", "GET /api/core/auth/oauth2/naver",
            "GET /api/core/auth/oauth2/kakao", "GET /api/core/auth/oauth2/status",
            "POST /api/core/auth/oauth2/exchange",
            "GET /api/core/hobbies", "GET /api/core/hobbies/simple",
            "GET /api/core/hobbies/categories", "GET /api/core/market/products/all",
            "POST /api/core/market/products/all/filter");

    @Value("${jwt.secret}")
    private String secretKey;

    public static class Config { }

    public JwtAuthenticationFilter(ObjectMapper objectMapper) {
        super(Config.class);
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (invalidPath(request.getURI().getRawPath())) {
                return writeError(exchange, ApiErrorCode.INVALID_PATH);
            }

            List<String> authorizations = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
            if (authorizations.size() > 1) {
                return writeError(exchange, ApiErrorCode.AUTH_INVALID);
            }
            String rawAuth = authorizations.size() == 1 ? authorizations.get(0) : null;
            boolean isMissing = rawAuth == null || rawAuth.isBlank();

            if (isMissing) {
                if (!isPublic(request)) {
                    return writeError(exchange, ApiErrorCode.AUTH_REQUIRED);
                }
                return chain.filter(exchange);
            }

            ApiErrorCode errorCode = validateToken(rawAuth);
            if (errorCode != null) {
                return writeError(exchange, errorCode);
            }
            return chain.filter(exchange);
        };
    }

    private boolean isPublic(ServerHttpRequest request) {
        String method = request.getMethod() == null ? "" : request.getMethod().name();
        String path = request.getPath().value();
        return PUBLIC_ROUTES.contains(method + " " + path)
                || ("GET".equals(method) && (path.matches("/api/core/hobbies/categories/[0-9]+")
                || path.matches("/api/core/profiles/user/[^/]+")
                || path.matches("/api/core/profiles/image/[^/]+")
                || path.matches("/api/core/market/products/[0-9]+")
                || path.matches("/api/core/market/products/images/[^/]+")
                || path.matches("/api/core/auth/oauth2/(authorize|callback)/(google|naver|kakao)")));
    }

    private ApiErrorCode validateToken(String authorization) {
        if (!authorization.startsWith("Bearer ") || authorization.length() == 7) return ApiErrorCode.AUTH_INVALID;
        try {
            String token = authorization.substring(7);
            String encoded = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
            SecretKey key = Keys.hmacShaKeyFor(encoded.getBytes(StandardCharsets.UTF_8));
            String subject = Jwts.parserBuilder().setSigningKey(key).build()
                    .parseClaimsJws(token).getBody().getSubject();
            return subject == null || subject.isBlank() ? ApiErrorCode.AUTH_INVALID : null;
        } catch (ExpiredJwtException exception) {
            return ApiErrorCode.AUTH_EXPIRED;
        } catch (Exception exception) {
            return ApiErrorCode.AUTH_INVALID;
        }
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ApiErrorCode code) {
        return GatewayErrorHandler.writeError(exchange, objectMapper, code);
    }

    private boolean invalidPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return path.contains("//") || path.indexOf('\\') >= 0 || path.endsWith("/")
                || path.equals("/.") || path.equals("/..") || path.contains("/./") || path.contains("/../")
                || lower.matches(".*%(?:2f|5c|00|2e|252f|255c|2500|252e).*");
    }
}
