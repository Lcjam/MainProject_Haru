package com.example.demo.exception;

import com.example.demo.filter.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = GatewayErrorContractHttpTest.TestApplication.class,
        properties = "jwt.secret=gateway-c01-contract-secret-long-enough")
class GatewayErrorContractHttpTest {
    private static final String SECRET = "gateway-c01-contract-secret-long-enough";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNode FIXTURE = fixture();
    private static final int CLOSED_PORT = closedPort();
    private static final String CORE_ERROR = "{ \"status\": \"error\", \"code\": \"INTERNAL_ERROR\", \"message\": \"서버 오류가 발생했습니다.\", \"coreField\": 123 }";
    private static final String ASSIST_ERROR = "{\"status\":\"error\",\"code\":\"BAD_REQUEST\",\"message\":\"입력값을 확인해주세요.\",\"assistField\":true}";
    private static final HttpServer BACKEND = backend();
    private final HttpClient client = HttpClient.newHttpClient();
    @LocalServerPort int port;

    @AfterAll static void stopBackend() { BACKEND.stop(0); }

    static Stream<Arguments> scenarios() {
        return StreamSupport.stream(FIXTURE.get("scenarios").spliterator(), false)
                .filter(s -> "gateway".equals(s.get("origin").asText()))
                .map(s -> Arguments.of(s.get("id").asText(), s));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void allGatewayFixtureScenariosUseActualHttp(String id, JsonNode scenario) throws Exception {
        HttpResponse<String> response = switch (id) {
            case "gateway.auth.missing" -> get("/api/core/probe/ok");
            case "gateway.auth.expired" -> get("/api/core/probe/ok", "Bearer " + token(-60000));
            case "gateway.auth.invalid" -> get("/api/core/probe/ok", "Bearer forged");
            case "gateway.path.invalid" -> get("/api/core/probe/ok/", "Bearer " + token(60000));
            case "gateway.route.notFound" -> get("/no-such-route");
            case "gateway.upstream.unavailable" -> get("/api/core/probe/offline", "Bearer " + token(60000));
            default -> throw new AssertionError("Unimplemented gateway fixture: " + id);
        };
        assertContract(response, scenario.get("http").asInt(), scenario.get("code").asText());
    }

    @Test
    void blankMalformedDuplicateAndEncodedPathRequestsAreRejected() throws Exception {
        assertContract(get("/api/core/probe/ok", "   "), 401, "AUTH_REQUIRED");
        assertContract(get("/api/core/probe/ok", "Basic abc"), 401, "AUTH_INVALID");
        assertContract(get("/api/core/probe/ok", "Bearer " + token(60000) + "tampered"), 401, "AUTH_INVALID");
        assertContract(get("/api/core/probe/ok", "Bearer " + token(60000), "Bearer forged"), 401, "AUTH_INVALID");
        assertContract(get("/api/core/probe/%2fprivate", "Bearer " + token(60000)), 400, "INVALID_PATH");
        assertContract(get("/api/core/../x", "Bearer " + token(60000)), 400, "INVALID_PATH");
    }

    @Test
    void timeoutAndUnhandledExceptionUseSafeMessages() throws Exception {
        assertContract(get("/test/slow"), 504, "UPSTREAM_TIMEOUT");
        assertContract(get("/test/internal"), 500, "INTERNAL_ERROR");
    }

    @Test
    void downstreamCoreAndAssistErrorBodiesAreNotRewritten() throws Exception {
        var core = get("/pass/core");
        assertEquals(500, core.statusCode());
        assertEquals(CORE_ERROR, core.body());
        var assist = get("/pass/assist");
        assertEquals(400, assist.statusCode());
        assertEquals(ASSIST_ERROR, assist.body());
    }

    private void assertContract(HttpResponse<String> response, int status, String code) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        JsonNode body = JSON.readTree(response.body());
        assertEquals("error", body.path("status").asText());
        assertEquals(code, body.path("code").asText());
        assertFalse(body.path("message").asText().isBlank());
        JsonNode definition = FIXTURE.get("codes").get(code);
        if (definition.get("fixedMessage").asBoolean()) assertEquals(definition.get("message").asText(), body.path("message").asText());
        for (String secret : List.of("Exception", "at com.", "SQL", "timestamp", "stackTrace")) assertFalse(response.body().contains(secret), response.body());
    }

    private HttpResponse<String> get(String path, String... authorization) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET();
        for (String value : authorization) request.header("Authorization", value);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String token(long expiry) {
        byte[] key = Base64.getEncoder().encodeToString(SECRET.getBytes(StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        return Jwts.builder().setSubject("fixture@example.com").setExpiration(new Date(System.currentTimeMillis() + expiry))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }
    private static JsonNode fixture() {
        try { return JSON.readTree(Path.of("../contracts/api-error-fixtures.json").toFile()); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static int closedPort() {
        try (var socket = new ServerSocket(0)) { return socket.getLocalPort(); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static HttpServer backend() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (path.equals("/slow")) {
                    try { Thread.sleep(300); } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
                }
                int status = path.equals("/core-error") ? 500 : path.equals("/assist-error") ? 400 : 200;
                byte[] body = (status == 500 ? CORE_ERROR : status == 400 ? ASSIST_ERROR : "ok").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json;charset=UTF-8");
                exchange.sendResponseHeaders(status, body.length);
                try (var stream = exchange.getResponseBody()) { stream.write(body); }
            });
            server.start(); return server;
        } catch (Exception error) { throw new IllegalStateException(error); }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class})
    @Import({JwtAuthenticationFilter.class, GatewayErrorHandler.class})
    static class TestApplication {
        @Bean RouteLocator routes(RouteLocatorBuilder builder, JwtAuthenticationFilter filter) {
            String backend = "http://127.0.0.1:" + BACKEND.getAddress().getPort();
            return builder.routes()
                    .route("offline", r -> r.path("/api/core/probe/offline")
                            .filters(f -> f.filter(filter.apply(new JwtAuthenticationFilter.Config())))
                            .uri("http://127.0.0.1:" + CLOSED_PORT))
                    .route("probe", r -> r.path("/api/core/**").filters(f -> f.filter(filter.apply(new JwtAuthenticationFilter.Config()))).uri(backend))
                    .route("slow", r -> r.path("/test/slow").filters(f -> f.setPath("/slow")).metadata("response-timeout", 25L).uri(backend))
                    .route("internal", r -> r.path("/test/internal").filters(f -> f.filter((exchange, chain) -> Mono.error(new IllegalStateException("SQL Exception at com.private")))).uri(backend))
                    .route("core-error", r -> r.path("/pass/core").filters(f -> f.setPath("/core-error")).uri(backend))
                    .route("assist-error", r -> r.path("/pass/assist").filters(f -> f.setPath("/assist-error")).uri(backend))
                    .build();
        }
    }
}
