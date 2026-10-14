package com.example.demo.filter;

import com.example.demo.dto.CommonResponseDTO;
import com.example.demo.util.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AssistAuthenticationFilter extends OncePerRequestFilter {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final TokenUtils tokens;

    public AssistAuthenticationFilter(TokenUtils tokens) {
        this.tokens = tokens;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (invalidPath(path)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<String> headers = Collections.list(request.getHeaders("Authorization"));
        if (headers.size() > 1) {
            writeError(response, "AUTH_INVALID");
            return;
        }
        String rawAuth = headers.size() == 1 ? headers.get(0) : null;
        boolean isMissing = rawAuth == null || rawAuth.isBlank();

        if (isMissing) {
            if (!isPublic(request.getMethod(), path)) {
                writeError(response, "AUTH_REQUIRED");
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        String error = tokens.authenticationErrorCode(rawAuth);
        if (error != null) {
            writeError(response, error);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isPublic(String method, String path) {
        return "POST".equals(method) && ("/api/assist/sms/send-sms".equals(path)
                || "/api/assist/sms/verify-otp".equals(path));
    }

    private void writeError(HttpServletResponse response, String code) throws IOException {
        String message = switch (code) {
            case "AUTH_EXPIRED" -> "인증이 만료되었습니다.";
            case "AUTH_REQUIRED" -> "로그인이 필요합니다.";
            default -> "유효하지 않은 인증입니다.";
        };
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getWriter(), CommonResponseDTO.builder()
                .status("error").code(code).message(message).build());
    }

    private boolean invalidPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return path.contains("//") || path.indexOf('\\') >= 0 || path.endsWith("/")
                || path.equals("/.") || path.equals("/..") || path.contains("/./") || path.contains("/../")
                || lower.matches(".*%(?:2f|5c|00|2e|252f|255c|2500|252e).*");
    }
}
