package com.example.demo.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = GatewayAuthOriginHttpTest.TestApplication.class,
        properties = "jwt.secret=gateway-http-origin-secret-long-enough")
class GatewayAuthOriginHttpTest {
    private static final String SECRET = "gateway-http-origin-secret-long-enough";
    private static final HttpServer BACKEND = startBackend();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @LocalServerPort
    int port;

    @AfterAll
    static void stopBackend() { BACKEND.stop(0); }

    @Test
    void proxyPortReturnsSharedAuthenticationErrors() throws Exception {
        assertError("/api/core/users/me", null, "AUTH_REQUIRED");
        assertError("/api/core/users/me", "Bearer " + token(-60_000), "AUTH_EXPIRED");
        assertError("/api/core/users/me", "Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
        assertError("/api/core/users/me", "Basic abc", "AUTH_INVALID");
        assertError("/api/core/auth/login", "Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
        assertEquals(200, get("/api/core/users/me", "Bearer " + token(60_000)).statusCode());
        assertEquals(200, post("/api/core/auth/login", null).statusCode());
        assertEquals(401, get("/api/core/auth/login", null).statusCode());
    }

    private void assertError(String path, String authorization, String code) throws Exception {
        var response = get(path, authorization);
        assertEquals(401, response.statusCode());
        var body = mapper.readTree(response.body());
        assertEquals("error", body.get("status").asText());
        assertEquals(code, body.get("code").asText());
        assertFalse(body.get("message").asText().isBlank());
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (authorization != null) builder.header("Authorization", authorization);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path, String authorization) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.noBody());
        if (authorization != null) builder.header("Authorization", authorization);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String token(long lifetimeMillis) {
        byte[] key = Base64.getEncoder().encodeToString(SECRET.getBytes(StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        return Jwts.builder().setSubject("a@haru.test")
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMillis))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }

    private static HttpServer startBackend() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class,
            ReactiveUserDetailsServiceAutoConfiguration.class})
    @Import(JwtAuthenticationFilter.class)
    static class TestApplication {
        @Bean RouteLocator routes(RouteLocatorBuilder builder, JwtAuthenticationFilter filter) {
            return builder.routes().route("probe", route -> route.path("/api/core/**")
                    .filters(filters -> filters.filter(filter.apply(new JwtAuthenticationFilter.Config())))
                    .uri("http://127.0.0.1:" + BACKEND.getAddress().getPort())).build();
        }
    }
}
