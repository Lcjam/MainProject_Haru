package com.example.demo.security;

import com.example.demo.config.OAuth2Config;
import com.example.demo.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.BindException;
import java.net.URI;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.net.CookieManager;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AuthOriginHttpTest.TestApplication.class)
class AuthOriginHttpTest {
    private static final String SECRET = "core-http-origin-secret-long-enough";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @LocalServerPort
    int port;

    @Test
    void directPortDistinguishesMissingExpiredForgedAndValidCredentials() throws Exception {
        assertError(null, "AUTH_REQUIRED");
        assertError("Bearer " + new JwtTokenProvider(SECRET, -60_000)
                .createToken(1, "a@haru.test", List.of("ROLE_USER")), "AUTH_EXPIRED");
        assertError("Bearer " + new JwtTokenProvider(SECRET, 60_000)
                .createToken(1, "a@haru.test", List.of("ROLE_USER")) + "tampered", "AUTH_INVALID");
        assertError("Basic abc", "AUTH_INVALID");
        assertEquals(200, get("/api/core/profiles/me", "Bearer " + new JwtTokenProvider(SECRET, 60_000)
                .createToken(1, "a@haru.test", List.of("ROLE_USER"))).statusCode());
        assertEquals(401, get("/api/core/auth/login", null).statusCode());
    }

    @Test
    void classpathStaticFilesCannotBypassImageAuthorization() throws Exception {
        String token = "Bearer " + new JwtTokenProvider(SECRET, 60_000)
                .createToken(1, "other@haru.test", List.of("ROLE_USER"));
        assertEquals(404, get("/profile-images/t_d315ba2e-0b54-4f44-962b-4d9710709cf3.png", token).statusCode());
        assertEquals(404, get("/uploads/product_test@test.test/product_91/23b0cb73-efe6-4df8-9aa1-6a8ad8e4f82a_image_0292_1440x1600.png", token).statusCode());
    }

    @Test
    void directPortKeepsOnlyListedPublicMethodsAndIgnoresForgedIdentityHeader() throws Exception {
        String other = "Bearer " + new JwtTokenProvider(SECRET, 60_000)
                .createToken(2, "other@haru.test", List.of("ROLE_USER"));
        assertEquals(200, get("/api/core/market/products/all", null).statusCode());
        assertEquals(401, get("/api/core/market/products/requests/approved", null).statusCode());
        assertEquals(401, get("/api/core/chat/messages", null).statusCode());
        assertEquals(401, get("/api/core/boards/30/members", null).statusCode());
        assertEquals(401, get("/api/core/market/products/all", "Bearer forged").statusCode());
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/core/chat/messages"))
                .header("Authorization", other).header("X-User-Email", "owner@haru.test").GET().build();
        assertEquals("other@haru.test", client.send(request, HttpResponse.BodyHandlers.ofString()).body());
    }

    @Test
    void oauthSessionDoesNotAuthenticateOrdinaryApi() throws Exception {
        HttpClient sessionClient = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        URI base = URI.create("http://localhost:" + port);
        assertEquals(200, sessionClient.send(HttpRequest.newBuilder(base.resolve("/api/core/auth/oauth2/session-probe"))
                .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals(401, sessionClient.send(HttpRequest.newBuilder(base.resolve("/api/core/profiles/me"))
                .GET().build(), HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void oauthSessionCookieUsesHttpOnlySameSiteAndSecureForForwardedHttps() throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/core/auth/oauth2/session-probe"))
                .header("X-Forwarded-Proto", "https").GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        String cookie = response.headers().firstValue("Set-Cookie").orElse("").toLowerCase();
        assertTrue(cookie.contains("httponly"));
        assertTrue(cookie.contains("samesite=lax"));
        assertTrue(cookie.contains("secure"));
    }

    @Test
    void oauthSessionCookieSupportsLocalHttpWithoutSecure() throws Exception {
        var response = get("/api/core/auth/oauth2/session-probe", null);
        assertEquals(200, response.statusCode());
        String cookie = response.headers().firstValue("Set-Cookie").orElse("").toLowerCase(Locale.ROOT);
        assertTrue(cookie.contains("httponly"));
        assertTrue(cookie.contains("samesite=lax"));
        assertFalse(cookie.contains("secure"));
    }

    @Test
    void oauthSessionCookieIgnoresForwardedHttpsFromUntrustedPeer() throws Exception {
        // 127.0.0.2 reaches the server but is outside the local Gateway allowlist.
        try (Socket socket = new Socket()) {
            try {
                socket.bind(new InetSocketAddress("127.0.0.2", 0));
            } catch (BindException exception) {
                Assumptions.assumeTrue(false, "Loopback alias 127.0.0.2 is unavailable: " + exception.getMessage());
            }
            socket.connect(new InetSocketAddress("127.0.0.1", port), 5_000);
            socket.setSoTimeout(5_000);
            String request = "GET /api/core/auth/oauth2/session-probe HTTP/1.1\r\n"
                    + "Host: localhost:" + port + "\r\n"
                    + "X-Forwarded-Proto: https\r\nX-Forwarded-For: 127.0.0.1\r\nConnection: close\r\n\r\n";
            socket.getOutputStream().write(request.getBytes(StandardCharsets.US_ASCII));
            String response = new String(socket.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(response.startsWith("HTTP/1.1 200"));
            String cookie = response.lines().filter(line -> line.toLowerCase(Locale.ROOT).startsWith("set-cookie:"))
                    .findFirst().orElse("").toLowerCase(Locale.ROOT);
            assertTrue(cookie.contains("httponly"));
            assertTrue(cookie.contains("samesite=lax"));
            assertFalse(cookie.contains("secure"));
        }
    }

    private void assertError(String authorization, String code) throws Exception {
        var response = get("/api/core/profiles/me", authorization);
        assertEquals(401, response.statusCode());
        var body = mapper.readTree(response.body());
        assertEquals("error", body.get("status").asText());
        assertEquals(code, body.get("code").asText());
        assertEquals(false, body.get("message").asText().isBlank());
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (authorization != null) builder.header("Authorization", authorization);
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class, SecurityAutoConfiguration.class,
            OAuth2ClientAutoConfiguration.class})
    @Import({SecurityConfig.class, ProbeController.class})
    static class TestApplication {
        @Bean JwtTokenProvider jwtTokenProvider() { return new JwtTokenProvider(SECRET, 60_000); }
        @Bean JwtTokenBlacklistService blacklistService() { return mock(JwtTokenBlacklistService.class); }
        @Bean JwtAuthenticationEntryPoint entryPoint() { return new JwtAuthenticationEntryPoint(); }
        @Bean OAuth2Config oauth2Config() { return mock(OAuth2Config.class); }
    }

    @RestController
    static class ProbeController {
        @GetMapping({"/api/core/profiles/me", "/api/core/auth/login"})
        ResponseEntity<String> probe() { return ResponseEntity.ok("ok"); }

        @GetMapping("/api/core/market/products/all")
        ResponseEntity<String> publicProducts() { return ResponseEntity.ok("public"); }

        @GetMapping("/api/core/chat/messages")
        ResponseEntity<String> protectedActor(org.springframework.security.core.Authentication actor) {
            return ResponseEntity.ok(actor.getName());
        }

        @GetMapping("/api/core/auth/oauth2/session-probe")
        ResponseEntity<String> oauthSession(jakarta.servlet.http.HttpServletRequest request) {
            var context = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    "owner@haru.test", "ignored", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
            request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
            return ResponseEntity.ok("session");
        }
    }
}
