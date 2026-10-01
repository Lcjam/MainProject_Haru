package com.example.demo.security;

import com.example.demo.controller.LlamaController;
import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.filter.AssistAuthenticationFilter;
import com.example.demo.serviceimpl.LlamaServiceImpl;
import com.example.demo.util.TokenUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AssistAuthOriginHttpTest.TestApplication.class)
class AssistAuthOriginHttpTest {
    private static final String SECRET = "assist-http-origin-secret-long-enough";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @LocalServerPort
    int port;

    @Test
    void directPortReturnsSharedAuthenticationErrors() throws Exception {
        assertError(null, "AUTH_REQUIRED");
        assertError("Bearer " + token(-60_000), "AUTH_EXPIRED");
        assertError("Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
        assertError("Basic abc", "AUTH_INVALID");
        assertEquals(200, post("Bearer " + token(60_000)).statusCode());
    }

    @Test
    void directPortUsesMethodAndPathWhitelistAndNeverTrustsIdentityHeaders() throws Exception {
        assertEquals(404, request("POST", "/api/assist/sms/send-sms", null, null).statusCode());
        assertError("/api/assist/sms/send-sms", "Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
        assertEquals(401, request("GET", "/api/assist/sms/send-sms", null, null).statusCode());
        assertEquals(401, request("POST", "/api/assist/tinylamanaver/chat", null, "owner@haru.test").statusCode());
    }

    private void assertError(String authorization, String code) throws Exception {
        var response = request("POST", "/api/assist/tinylamanaver/chat", authorization, null);
        assertEquals(401, response.statusCode());
        var body = mapper.readTree(response.body());
        assertEquals("error", body.get("status").asText());
        assertEquals(code, body.get("code").asText());
        assertFalse(body.get("message").asText().isBlank());
    }

    private HttpResponse<String> post(String authorization) throws Exception {
        return request("POST", "/api/assist/tinylamanaver/chat", authorization, null);
    }

    private HttpResponse<String> request(String method, String path, String authorization, String identity) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString("{\"message\":\"hello\"}"));
        if (authorization != null) builder.header("Authorization", authorization);
        if (identity != null) builder.header("X-User-Email", identity);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String token(long lifetimeMillis) {
        byte[] key = Base64.getEncoder().encodeToString(SECRET.getBytes(StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        return Jwts.builder().setSubject("a@haru.test")
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMillis))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({LlamaController.class, AssistAuthenticationFilter.class})
    static class TestApplication {
        @Bean TokenUtils tokenUtils() { return new TokenUtils(SECRET); }
        @Bean LlamaServiceImpl llamaService() { return mock(LlamaServiceImpl.class); }
        @Bean ChatMessageDAO chatMessageDAO() { return mock(ChatMessageDAO.class); }
    }
}
