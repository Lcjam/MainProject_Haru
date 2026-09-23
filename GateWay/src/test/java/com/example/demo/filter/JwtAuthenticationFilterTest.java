package com.example.demo.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    @Test
    void publicPathContinuesTheReactiveChain() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/core/auth/login"));
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
    void protectedPathWithoutTokenReturnsUnauthorized() {
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
    }
}
