package com.example.demo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new ObjectMapper());

    JwtAuthenticationFilterTest() {
        ReflectionTestUtils.setField(filter, "secretKey", "test-secret-for-gateway-auth-contract");
    }

    @Test
    void exactPublicMethodAndPathContinueTheReactiveChain() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/core/auth/login"));
        var chainCalled = new AtomicBoolean();

        filter.apply(new JwtAuthenticationFilter.Config())
                .filter(exchange, ignored -> {
                    chainCalled.set(true);
                    return Mono.empty();
                })
                .block();

        assertTrue(chainCalled.get());
    }

    @Test
    void publicRoutesStillRejectPresentedInvalidCredentialsAndNearMissesRequireJwt() throws Exception {
        assertError("POST", "/api/core/auth/login", "Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
        assertError("GET", "/api/core/auth/login", null, "AUTH_REQUIRED");
        assertError("GET", "/api/core/hobbies/user", null, "AUTH_REQUIRED");
    }

    @Test
    void nonCanonicalPublicPathIsRejectedBeforeItCanMatch() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/core/auth/login/"));
        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, ignored -> Mono.empty()).block();
        assertEquals(HttpStatus.BAD_REQUEST, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedPathWithoutTokenReturnsUnauthorized() throws Exception {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/core/users/me"));
        var chainCalled = new AtomicBoolean();

        filter.apply(new JwtAuthenticationFilter.Config())
                .filter(exchange, ignored -> {
                    chainCalled.set(true);
                    return Mono.empty();
                })
                .block();

        assertFalse(chainCalled.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals("AUTH_REQUIRED", new ObjectMapper().readTree(exchange.getResponse().getBodyAsString().block()).get("code").asText());
    }

    @Test
    void expiredAndForgedTokensHaveDistinctCodes() throws Exception {
        assertError("Bearer " + token(-60_000), "AUTH_EXPIRED");
        assertError("Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
    }

    @Test
    void malformedAuthorizationHeaderIsInvalidRatherThanMissing() throws Exception {
        assertError("Basic abc", "AUTH_INVALID");
        assertError("Bearer", "AUTH_INVALID");
        assertError("Bearer ", "AUTH_INVALID");
    }

    @Test
    void validTokenContinuesTheChain() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/core/users/me")
                .header("Authorization", "Bearer " + token(60_000)));
        var called = new AtomicBoolean();
        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, ignored -> {
            called.set(true);
            return Mono.empty();
        }).block();
        assertTrue(called.get());
    }

    @Test
    void blankAuthorizationHeaderIsTreatedAsMissing() throws Exception {
        assertError("", "AUTH_REQUIRED");
        assertError("   ", "AUTH_REQUIRED");

        var exchangeEmpty = MockServerWebExchange.from(MockServerHttpRequest.post("/api/core/auth/login")
                .header("Authorization", ""));
        var calledEmpty = new AtomicBoolean();
        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchangeEmpty, ignored -> {
            calledEmpty.set(true);
            return Mono.empty();
        }).block();
        assertTrue(calledEmpty.get());

        var exchangeWhitespace = MockServerWebExchange.from(MockServerHttpRequest.post("/api/core/auth/login")
                .header("Authorization", "   "));
        var calledWhitespace = new AtomicBoolean();
        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchangeWhitespace, ignored -> {
            calledWhitespace.set(true);
            return Mono.empty();
        }).block();
        assertTrue(calledWhitespace.get());
    }

    private void assertError(String authorization, String code) throws Exception {
        assertError("GET", "/api/core/users/me", authorization, code);
    }

    private void assertError(String method, String path, String authorization, String code) throws Exception {
        var request = MockServerHttpRequest.method(method, path);
        if (authorization != null) request.header("Authorization", authorization);
        var exchange = MockServerWebExchange.from(request);
        filter.apply(new JwtAuthenticationFilter.Config()).filter(exchange, ignored -> Mono.empty()).block();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        var body = new ObjectMapper().readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals("error", body.get("status").asText());
        assertEquals(code, body.get("code").asText());
        assertTrue(body.get("message").asText().length() > 0);
    }

    private String token(long lifetimeMillis) {
        String secret = "test-secret-for-gateway-auth-contract";
        byte[] key = Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        return Jwts.builder().setSubject("tester@example.com")
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMillis))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }
}
