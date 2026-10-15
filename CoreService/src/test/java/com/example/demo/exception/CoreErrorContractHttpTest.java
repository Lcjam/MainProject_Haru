package com.example.demo.exception;

import com.example.demo.config.OAuth2Config;
import com.example.demo.config.SecurityConfig;
import com.example.demo.controller.AuthController;
import com.example.demo.controller.ProfileController;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.*;
import com.example.demo.util.TokenUtils;
import com.example.demo.util.BaseResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = CoreErrorContractHttpTest.TestApplication.class,
        properties = {"spring.servlet.multipart.max-file-size=128B", "spring.servlet.multipart.max-request-size=256B"})
class CoreErrorContractHttpTest {
    private static final String SECRET = "core-c01-contract-secret-long-enough";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final JsonNode FIXTURE = fixture();
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String SIGNUP = """
            {"name":"테스트","email":"new@example.com","nickname":"새닉네임",
             "password":"password123","phoneNumber":"01000000000"}
            """;
    @LocalServerPort int port;
    @Autowired UserMapper userMapper;

    @BeforeEach
    void resetDatabaseMock() { reset(userMapper); }

    static Stream<Arguments> scenarios() {
        return StreamSupport.stream(FIXTURE.get("scenarios").spliterator(), false)
                .filter(s -> "core".equals(s.get("origin").asText()))
                .map(s -> Arguments.of(s.get("id").asText(), s));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void allCoreFixtureScenariosUseActualHttp(String id, JsonNode scenario) throws Exception {
        HttpResponse<String> response = switch (id) {
            case "core.auth.missing" -> send("GET", "/api/core/contracts/ok", null, null, null);
            case "core.auth.expired" -> send("GET", "/api/core/contracts/ok", bearer(-60000), null, null);
            case "core.auth.invalid" -> send("GET", "/api/core/contracts/ok", "Bearer forged", null, null);
            case "core.validation" -> send("POST", "/api/core/auth/login", null, "application/json", "{}");
            case "core.malformedJson" -> send("POST", "/api/core/auth/login", null, "application/json", "{");
            case "core.methodNotAllowed" -> send("PATCH", "/api/core/contracts/ok", bearer(60000), null, null);
            case "core.route.notFound" -> send("GET", "/api/core/contracts/missing", bearer(60000), null, null);
            case "core.forbidden" -> send("GET", "/api/core/contracts/forbidden", bearer(60000), null, null);
            case "core.legacyNumeric" -> send("GET", "/api/core/contracts/legacy", bearer(60000), null, null);
            case "core.payloadTooLarge" -> send("POST", "/api/core/contracts/upload", bearer(60000),
                    "multipart/form-data; boundary=fixture", "--fixture\r\nContent-Disposition: form-data; name=\"file\"; filename=\"x.txt\"\r\nContent-Type: text/plain\r\n\r\n" + "x".repeat(1024) + "\r\n--fixture--\r\n");
            case "core.signup.duplicateEmail" -> duplicateSignup(ApiErrorCode.DUPLICATE_EMAIL);
            case "core.signup.duplicateNickname" -> duplicateSignup(ApiErrorCode.DUPLICATE_NICKNAME);
            case "core.internal" -> send("GET", "/api/core/contracts/internal", bearer(60000), null, null);
            default -> throw new AssertionError("Unimplemented core fixture: " + id);
        };
        assertContract(response, scenario.get("http").asInt(), scenario.get("code").asText());
    }

    private HttpResponse<String> duplicateSignup(ApiErrorCode code) throws Exception {
        String message;
        if (code == ApiErrorCode.DUPLICATE_EMAIL) {
            when(userMapper.findByEmail("new@example.com")).thenReturn(new User());
            message = "이미 존재하는 이메일입니다.";
        } else {
            when(userMapper.findByNickname("새닉네임")).thenReturn(new User());
            message = "이미 존재하는 닉네임입니다.";
        }
        var response = send("POST", "/api/core/auth/signup", null, "application/json", SIGNUP);
        assertEquals(message, JSON.readTree(response.body()).path("message").asText());
        verify(userMapper, never()).insertUser(any());
        return response;
    }

    @Test
    void loginFailurePromotesBusinessMessage() throws Exception {
        var response = send("POST", "/api/core/auth/login", null, "application/json",
                "{\"email\":\"missing@example.com\",\"password\":\"wrong-password\"}");
        assertBusinessError(response, 400, "BAD_REQUEST", "잘못된 이메일 또는 비밀번호입니다.");
    }

    @Test
    void profileNicknameDuplicatePromotesBusinessMessage() throws Exception {
        when(userMapper.findByEmail("fixture@example.com")).thenReturn(User.builder().nickname("기존닉네임").build());
        when(userMapper.findByNicknameExceptEmail("중복닉네임", "fixture@example.com")).thenReturn(new User());
        var response = send("PUT", "/api/core/profiles/me", bearer(60000), "application/json", "{\"nickname\":\"중복닉네임\"}");
        assertBusinessError(response, 409, "DUPLICATE_NICKNAME", "이미 사용 중인 닉네임입니다.");
        verify(userMapper, never()).updateUserProfile(any());
    }

    @Test
    void signupPhoneDuplicateAndInvalidHobbiesKeepSpecificMessages() throws Exception {
        when(userMapper.findByPhoneNumber("01000000000")).thenReturn(new User());
        assertBusinessError(send("POST", "/api/core/auth/signup", null, "application/json", SIGNUP),
                409, "DUPLICATE_PHONE", "이미 등록된 전화번호입니다.");
        reset(userMapper);
        String invalidHobbies = SIGNUP.stripTrailing().replace("}", ",\"hobbies\":[{\"hobbyId\":1}]}");
        assertBusinessError(send("POST", "/api/core/auth/signup", null, "application/json", invalidHobbies),
                400, "BAD_REQUEST", "카테고리 정보가 누락되었습니다.");
        verify(userMapper, never()).insertUser(any());
    }

    @Test
    void signupAndProfileSuccessKeepExistingJsonShape() throws Exception {
        when(userMapper.insertUser(any())).thenReturn(1);
        var signup = send("POST", "/api/core/auth/signup", null, "application/json", SIGNUP);
        assertEquals(200, signup.statusCode(), signup.body());
        JsonNode body = JSON.readTree(signup.body());
        assertEquals("success", body.path("status").asText());
        assertEquals("200", body.path("code").asText());
        var fields = new HashSet<String>();
        body.path("data").fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("success", "email", "message", "initialDopamine", "initialPoints"), fields);
        assertEquals(50, body.path("data").path("initialDopamine").asInt());
        assertEquals(0, body.path("data").path("initialPoints").asInt());

        when(userMapper.findByEmail("fixture@example.com")).thenReturn(User.builder()
                .email("fixture@example.com").nickname("기존닉네임").build());
        var profile = send("PUT", "/api/core/profiles/me", bearer(60000), "application/json", "{\"bio\":\"수정 소개\"}");
        assertEquals(200, profile.statusCode(), profile.body());
        body = JSON.readTree(profile.body());
        assertEquals("success", body.path("status").asText());
        assertEquals("200", body.path("code").asText());
        fields.clear();
        body.path("data").fieldNames().forEachRemaining(fields::add);
        assertEquals(Set.of("success", "message", "updatedProfile"), fields);
    }

    private void assertBusinessError(HttpResponse<String> response, int status, String code, String message) throws Exception {
        assertContract(response, status, code);
        JsonNode body = JSON.readTree(response.body());
        assertEquals(message, body.path("message").asText());
        assertEquals(message, body.path("data").path("message").asText());
        assertFalse(body.path("data").has("errorCode"));
    }

    @Test
    void missingParameterTypeConstraintAndMediaTypeUseTheirHttpCodes() throws Exception {
        assertContract(send("GET", "/api/core/contracts/parameter", bearer(60000), null, null), 400, "BAD_REQUEST");
        assertContract(send("GET", "/api/core/contracts/parameter?n=no", bearer(60000), null, null), 400, "BAD_REQUEST");
        assertContract(send("GET", "/api/core/contracts/parameter?n=0", bearer(60000), null, null), 400, "BAD_REQUEST");
        assertContract(send("POST", "/api/core/auth/login", null, "text/plain", "not-json"), 415, "UNSUPPORTED_MEDIA_TYPE");
    }

    @Test
    void missingRequiredHeaderUsesSafeBadRequest() throws Exception {
        var response = send("GET", "/api/core/contracts/header", bearer(60000), null, null);
        assertContract(response, 400, "BAD_REQUEST");
        assertEquals(ApiErrorCode.BAD_REQUEST.message(), JSON.readTree(response.body()).path("message").asText());
        assertFalse(response.body().contains("X-Required"));
    }

    @Test
    void methodNotAllowedPreservesAllowHeader() throws Exception {
        var response = send("PATCH", "/api/core/contracts/ok", bearer(60000), null, null);
        assertContract(response, 405, "METHOD_NOT_ALLOWED");
        Set<String> allowed = response.headers().allValues("Allow").stream()
                .flatMap(value -> java.util.Arrays.stream(value.split(",")))
                .map(String::trim).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("GET"), allowed);
    }

    @Test
    void numberFormatExceptionDoesNotExposeInternalMessage() throws Exception {
        var response = send("GET", "/api/core/contracts/number-format", bearer(60000), null, null);
        assertContract(response, 400, "BAD_REQUEST");
        assertEquals(ApiErrorCode.BAD_REQUEST.message(), JSON.readTree(response.body()).path("message").asText());
        assertFalse(response.body().contains("For input string"));
        assertFalse(response.body().contains("abc"));
    }

    @Test
    void exactIllegalArgumentExceptionKeepsBusinessMessage() throws Exception {
        var response = send("GET", "/api/core/contracts/business-argument", bearer(60000), null, null);
        assertContract(response, 400, "BAD_REQUEST");
        assertEquals("게시판의 멤버만 게시글을 작성할 수 있습니다.", JSON.readTree(response.body()).path("message").asText());
    }

    @Test
    void unsupportedSpringStatusUsesExistingBadRequestFallback() throws Exception {
        var response = send("GET", "/api/core/contracts/not-acceptable", bearer(60000), null, null);
        assertContract(response, 400, "BAD_REQUEST");
        assertEquals(ApiErrorCode.BAD_REQUEST.message(), JSON.readTree(response.body()).path("message").asText());
    }

    @Test
    void springTimeoutUsesSafeServiceUnavailable() throws Exception {
        assertContract(send("GET", "/api/core/contracts/async-timeout", bearer(60000), null, null),
                503, "UPSTREAM_UNAVAILABLE");
    }

    @Test
    void securityAccessDeniedAndServletErrorUseContract() throws Exception {
        assertContract(send("GET", "/api/core/profiles/admin/probe", bearer(60000), null, null), 403, "FORBIDDEN");
        assertContract(send("GET", "/api/core/contracts/send-error", bearer(60000), null, null), 500, "INTERNAL_ERROR");
    }

    @Test
    void legacyBaseResponseAndInternalNumericControllerErrorsAreNormalized() throws Exception {
        assertContract(send("GET", "/api/core/contracts/base", bearer(60000), null, null), 500, "INTERNAL_ERROR");
        assertContract(send("GET", "/api/core/contracts/legacy-internal", bearer(60000), null, null), 500, "INTERNAL_ERROR");
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

    private HttpResponse<String> send(String method, String path, String token, String type, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        if (token != null) request.header("Authorization", token);
        if (type != null) request.header("Content-Type", type);
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static String bearer(long expiry) { return "Bearer " + new JwtTokenProvider(SECRET, expiry).createToken(1, "fixture@example.com", List.of("ROLE_USER")); }
    private static JsonNode fixture() {
        try { return JSON.readTree(Path.of("../contracts/api-error-fixtures.json").toFile()); }
        catch (Exception error) { throw new IllegalStateException(error); }
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class, SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class})
    @Import({SecurityConfig.class, AuthController.class, ProfileController.class, AuthService.class,
            ProfileService.class, TokenUtils.class, Probe.class, GlobalExceptionHandler.class,
            ApiErrorResponseAdvice.class, ApiErrorController.class})
    static class TestApplication {
        @Bean JwtTokenProvider tokens() { return new JwtTokenProvider(SECRET, 60000); }
        @Bean JwtTokenBlacklistService blacklist() { return mock(JwtTokenBlacklistService.class); }
        @Bean JwtAuthenticationEntryPoint entryPoint() { return new JwtAuthenticationEntryPoint(); }
        @Bean OAuth2Config oauth() { return mock(OAuth2Config.class); }
        @Bean UserMapper userMapper() { return mock(UserMapper.class); }
        @Bean HobbyService hobbies() { return mock(HobbyService.class); }
        @Bean ProfileImageService images() { return mock(ProfileImageService.class); }
        @Bean FileStorageService files() { return mock(FileStorageService.class); }
        @Bean WithdrawalService withdrawals() { return mock(WithdrawalService.class); }
        @Bean UserService users(AuthService auth, HobbyService hobbies, ProfileService profiles,
                ProfileImageService images, WithdrawalService withdrawals, TokenUtils tokens) {
            return new UserService(auth, hobbies, profiles, images, withdrawals, tokens);
        }
    }

    @Validated
    @RestController
    static class Probe {
        @GetMapping("/api/core/contracts/ok") public String ok() { return "ok"; }
        @GetMapping("/api/core/contracts/forbidden") public String forbidden() { throw new ForbiddenException("접근할 수 없습니다."); }
        @GetMapping("/api/core/contracts/legacy") public ApiResponse<?> legacy() { return ApiResponse.error("로그인이 필요합니다.", "401"); }
        @GetMapping("/api/core/contracts/internal") public String internal() { throw new IllegalStateException("SQL Exception at com.private"); }
        @GetMapping("/api/core/contracts/legacy-internal") public ApiResponse<?> legacyInternal() { return ApiResponse.error("SQL Exception at com.private", "500"); }
        @GetMapping("/api/core/contracts/base") public ResponseEntity<?> base() { return ResponseEntity.internalServerError().body(new BaseResponse<>(null, "SQL Exception at com.private")); }
        @GetMapping("/api/core/contracts/header") public String header(@RequestHeader("X-Required") String required) { return "ok"; }
        @GetMapping("/api/core/contracts/number-format") public String numberFormat() { return Integer.toString(Integer.parseInt("abc")); }
        @GetMapping("/api/core/contracts/business-argument") public String businessArgument() { throw new IllegalArgumentException("게시판의 멤버만 게시글을 작성할 수 있습니다."); }
        @GetMapping("/api/core/contracts/not-acceptable") public String notAcceptable() throws org.springframework.web.HttpMediaTypeNotAcceptableException { throw new org.springframework.web.HttpMediaTypeNotAcceptableException("internal media negotiation detail"); }
        @GetMapping("/api/core/contracts/async-timeout") public String asyncTimeout() { throw new org.springframework.web.context.request.async.AsyncRequestTimeoutException(); }
        @GetMapping("/api/core/contracts/parameter") public String parameter(@RequestParam @Min(1) int n) { return "ok"; }
        @PostMapping("/api/core/contracts/upload") public String upload(@RequestParam MultipartFile file) { return "ok"; }
        @GetMapping("/api/core/contracts/send-error") public void sendError(HttpServletResponse response) throws java.io.IOException { response.sendError(500, "SQL Exception at com.private"); }
        @GetMapping("/api/core/profiles/admin/probe") public String admin() { return "admin"; }
    }
}
