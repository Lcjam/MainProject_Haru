package com.example.demo.security;

import com.example.demo.advice.GlobalExceptionHandler;
import com.example.demo.common.ErrorCode;
import com.example.demo.controller.CustomErrorController;
import com.example.demo.filter.AssistAuthenticationFilter;
import com.example.demo.util.TokenUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = AssistApiErrorHttpTest.TestApplication.class,
        properties = {
                "spring.servlet.multipart.max-file-size=1KB",
                "spring.servlet.multipart.max-request-size=2KB"
        }
)
public class AssistApiErrorHttpTest {

    private static final String SECRET = "assist-http-contract-secret-value-long-enough-32bytes";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private JsonNode fixturesJson;

    @LocalServerPort
    int port;

    @RestController
    @RequestMapping("/api/assist")
    static class TestController {

        @PostMapping("/test/action")
        public Map<String, String> testAction(@RequestBody Map<String, Object> body) {
            return Map.of("result", "ok");
        }

        @GetMapping("/test/internal-error")
        public String throwInternalError() {
            throw new RuntimeException("Simulated internal database crash with secret details");
        }

        @PostMapping("/test/upload")
        public Map<String, String> uploadFile(@RequestParam("file") MultipartFile file) {
            return Map.of("filename", file.getOriginalFilename());
        }

        @GetMapping("/test/send-error-400")
        public void sendError400(HttpServletResponse response) throws IOException {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }

        @GetMapping("/test/send-error-403")
        public void sendError403(HttpServletResponse response) throws IOException {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

        @GetMapping("/test/illegal-argument-marker")
        public void throwIllegalArgumentMarker() {
            throw new IllegalArgumentException("provider-body=INTERNAL_MARKER");
        }

        @GetMapping("/test/response-status-marker")
        public void throwResponseStatusMarker() {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INTERNAL_REASON_MARKER");
        }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
    @Import({
            GlobalExceptionHandler.class,
            CustomErrorController.class,
            AssistAuthenticationFilter.class,
            TestController.class
    })
    static class TestApplication {
        @Bean
        TokenUtils tokenUtils() {
            return new TokenUtils(SECRET);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        File fixturesFile = new File("../contracts/api-error-fixtures.json");
        fixturesJson = OBJECT_MAPPER.readTree(fixturesFile);
    }

    private String createToken(long lifetimeMillis) {
        byte[] key = Base64.getEncoder().encodeToString(SECRET.getBytes(StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        return Jwts.builder()
                .setSubject("user@example.test")
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMillis))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256)
                .compact();
    }

    @Test
    @DisplayName("1. 계약 fixture의 origin=assist 7개 시나리오 전부 실제 HTTP 클라이언트로 검증")
    void testAllAssistScenariosFromContractFixtures_usingRealHttpClient() throws Exception {
        JsonNode scenarios = fixturesJson.path("scenarios");
        JsonNode codes = fixturesJson.path("codes");

        int assistScenariosCount = 0;
        for (JsonNode scenario : scenarios) {
            if (!"assist".equals(scenario.path("origin").asText())) {
                continue;
            }
            assistScenariosCount++;
            String id = scenario.path("id").asText();
            int expectedHttp = scenario.path("http").asInt();
            String expectedCode = scenario.path("code").asText();

            JsonNode codeMeta = codes.path(expectedCode);
            boolean fixedMessage = codeMeta.path("fixedMessage").asBoolean();
            String contractDefaultMessage = codeMeta.path("message").asText();

            HttpRequest request = switch (id) {
                case "assist.auth.missing" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/action"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"test\":\"data\"}"))
                        .build();
                case "assist.auth.expired" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/action"))
                        .header("Authorization", "Bearer " + createToken(-60_000))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"test\":\"data\"}"))
                        .build();
                case "assist.auth.invalid" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/action"))
                        .header("Authorization", "Bearer invalid.token.value")
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"test\":\"data\"}"))
                        .build();
                case "assist.malformedJson" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/action"))
                        .header("Authorization", "Bearer " + createToken(60_000))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{\"malformed\": json without close"))
                        .build();
                case "assist.methodNotAllowed" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/action"))
                        .header("Authorization", "Bearer " + createToken(60_000))
                        .GET()
                        .build();
                case "assist.route.notFound" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/non-existent-path"))
                        .header("Authorization", "Bearer " + createToken(60_000))
                        .GET()
                        .build();
                case "assist.internal" -> HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/internal-error"))
                        .header("Authorization", "Bearer " + createToken(60_000))
                        .GET()
                        .build();
                default -> throw new IllegalArgumentException("Unknown assist scenario id: " + id);
            };

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int actualStatus = response.statusCode();
            String body = response.body();

            assertThat(actualStatus)
                    .as("HTTP status for scenario " + id)
                    .isEqualTo(expectedHttp);

            JsonNode responseJson = OBJECT_MAPPER.readTree(body);
            assertThat(responseJson.path("status").asText())
                    .as("status in body for scenario " + id)
                    .isEqualTo("error");
            assertThat(responseJson.path("code").asText())
                    .as("code in body for scenario " + id)
                    .isEqualTo(expectedCode);

            String actualMessage = responseJson.path("message").asText();
            if (fixedMessage) {
                assertThat(actualMessage)
                        .as("Exact message match for fixedMessage code " + expectedCode)
                        .isEqualTo(contractDefaultMessage);
            } else {
                assertThat(actualMessage)
                        .as("Non-empty message for code " + expectedCode)
                        .isNotBlank();
            }

            if ("assist.methodNotAllowed".equals(id)) {
                String allowHeader = response.headers().firstValue("Allow").orElse(null);
                assertThat(allowHeader)
                        .as("Allow header on 405 scenario " + id)
                        .isNotNull()
                        .contains("POST");
            }

            assertThat(body)
                    .as("Internal exception leakage check for scenario " + id)
                    .doesNotContain("Exception")
                    .doesNotContain("at com.")
                    .doesNotContain("at org.");
        }

        assertThat(assistScenariosCount).isEqualTo(7);
    }

    @Test
    @DisplayName("2. multipart 한도 초과 시 실제 HTTP 413 PAYLOAD_TOO_LARGE 반환")
    void testMultipartLimitExceeded_returns413PayloadTooLarge_usingRealHttpClient() throws Exception {
        String boundary = "----Boundary" + System.currentTimeMillis();
        byte[] fileBytes = new byte[3 * 1024]; // 3KB (limit configured to 2KB)
        Arrays.fill(fileBytes, (byte) 'A');

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Disposition: form-data; name=\"file\"; filename=\"large.txt\"\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        baos.write(fileBytes);
        baos.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        byte[] multipartBody = baos.toByteArray();

        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("Authorization", "Bearer " + createToken(60_000))
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(response.statusCode()).isEqualTo(413);
        JsonNode responseJson = OBJECT_MAPPER.readTree(response.body());
        assertThat(responseJson.path("status").asText()).isEqualTo("error");
        assertThat(responseJson.path("code").asText()).isEqualTo("PAYLOAD_TOO_LARGE");
        assertThat(responseJson.path("message").asText()).isEqualTo("요청 크기가 너무 큽니다.");
        assertThat(response.body())
                .doesNotContain("MaxUploadSizeExceededException")
                .doesNotContain("at org.");
    }

    @Test
    @DisplayName("3. 필터 및 컨트롤러 단계의 실제 컨테이너 /error 디스패치가 계약 봉투 반환")
    void testContainerErrorDispatch_returnsContractEnvelope_usingRealHttpClient() throws Exception {
        // 케이스 1: 필터 단계 invalidPath -> sendError(SC_BAD_REQUEST) -> 실제 컨테이너 /error 디스패치
        HttpRequest invalidPathRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/%2e%2e/test"))
                .header("Authorization", "Bearer " + createToken(60_000))
                .GET()
                .build();

        HttpResponse<String> filterErrorResponse = httpClient.send(invalidPathRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(filterErrorResponse.statusCode()).isEqualTo(400);

        JsonNode filterJson = OBJECT_MAPPER.readTree(filterErrorResponse.body());
        assertThat(filterJson.path("status").asText()).isEqualTo("error");
        assertThat(filterJson.path("code").asText()).isEqualTo("BAD_REQUEST");
        assertThat(filterJson.path("message").asText()).isEqualTo("잘못된 요청입니다.");

        // 케이스 2: 컨트롤러 단계 sendError(400) -> 실제 컨테이너 /error 디스패치
        HttpRequest controllerSendError400 = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/send-error-400"))
                .header("Authorization", "Bearer " + createToken(60_000))
                .GET()
                .build();

        HttpResponse<String> c400Response = httpClient.send(controllerSendError400, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(c400Response.statusCode()).isEqualTo(400);

        JsonNode c400Json = OBJECT_MAPPER.readTree(c400Response.body());
        assertThat(c400Json.path("status").asText()).isEqualTo("error");
        assertThat(c400Json.path("code").asText()).isEqualTo("BAD_REQUEST");
        assertThat(c400Json.path("message").asText()).isEqualTo("잘못된 요청입니다.");

        // 케이스 3: 컨트롤러 단계 sendError(403) -> 실제 컨테이너 /error 디스패치
        HttpRequest controllerSendError403 = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/send-error-403"))
                .header("Authorization", "Bearer " + createToken(60_000))
                .GET()
                .build();

        HttpResponse<String> c403Response = httpClient.send(controllerSendError403, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(c403Response.statusCode()).isEqualTo(403);

        JsonNode c403Json = OBJECT_MAPPER.readTree(c403Response.body());
        assertThat(c403Json.path("status").asText()).isEqualTo("error");
        assertThat(c403Json.path("code").asText()).isEqualTo("FORBIDDEN");
        assertThat(c403Json.path("message").asText()).isEqualTo("접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("4. 매핑되지 않은 정적 리소스 요청 시 실제 HTTP 404 NOT_FOUND 계약 봉투 반환")
    void testStaticResourceNotFound_returns404JsonEnvelope_usingRealHttpClient() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/static/non-existing-file.css"))
                .header("Authorization", "Bearer " + createToken(60_000))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(404);

        JsonNode json = OBJECT_MAPPER.readTree(response.body());
        assertThat(json.path("status").asText()).isEqualTo("error");
        assertThat(json.path("code").asText()).isEqualTo("NOT_FOUND");
        assertThat(json.path("message").asText()).isEqualTo("요청한 자원을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("5. 내부 마커(INTERNAL_MARKER, INTERNAL_REASON_MARKER)가 실제 HTTP 응답에 노출되지 않음")
    void testInternalMarkersNotLeakedInHttpResponses_usingRealHttpClient() throws Exception {
        // IllegalArgumentException marker
        HttpRequest illegalArgRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/illegal-argument-marker"))
                .header("Authorization", "Bearer " + createToken(60_000))
                .GET()
                .build();

        HttpResponse<String> illegalArgResponse = httpClient.send(illegalArgRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(illegalArgResponse.statusCode()).isEqualTo(400);

        JsonNode illegalArgJson = OBJECT_MAPPER.readTree(illegalArgResponse.body());
        assertThat(illegalArgJson.path("status").asText()).isEqualTo("error");
        assertThat(illegalArgJson.path("code").asText()).isEqualTo("BAD_REQUEST");
        assertThat(illegalArgJson.path("message").asText()).isEqualTo("잘못된 요청입니다.");
        assertThat(illegalArgResponse.body()).doesNotContain("INTERNAL_MARKER");

        // ResponseStatusException reason marker
        HttpRequest responseStatusRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/assist/test/response-status-marker"))
                .header("Authorization", "Bearer " + createToken(60_000))
                .GET()
                .build();

        HttpResponse<String> responseStatusResponse = httpClient.send(responseStatusRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(responseStatusResponse.statusCode()).isEqualTo(409);

        JsonNode responseStatusJson = OBJECT_MAPPER.readTree(responseStatusResponse.body());
        assertThat(responseStatusJson.path("status").asText()).isEqualTo("error");
        assertThat(responseStatusJson.path("code").asText()).isEqualTo("CONFLICT");
        assertThat(responseStatusJson.path("message").asText()).isEqualTo("요청이 현재 상태와 충돌합니다.");
        assertThat(responseStatusResponse.body()).doesNotContain("INTERNAL_REASON_MARKER");
    }
}
