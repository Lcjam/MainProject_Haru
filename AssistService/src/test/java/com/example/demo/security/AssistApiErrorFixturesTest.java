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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

public class AssistApiErrorFixturesTest {

    private static final String SECRET = "assist-contract-test-secret-value-long-enough-32bytes";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MockMvc mockMvc;
    private JsonNode fixturesJson;

    @RestController
    static class AssistTestController {
        @PostMapping("/api/assist/test/action")
        public Map<String, String> testAction(@RequestBody Map<String, Object> body) {
            return Map.of("result", "ok");
        }

        @GetMapping("/api/assist/test/internal-error")
        public String throwInternalError() {
            throw new RuntimeException("Simulated internal database crash with secret details");
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        TokenUtils tokenUtils = new TokenUtils(SECRET);
        AssistAuthenticationFilter filter = new AssistAuthenticationFilter(tokenUtils);

        mockMvc = MockMvcBuilders.standaloneSetup(new AssistTestController(), new CustomErrorController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(filter)
                .build();

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
    void testAllAssistScenariosFromContractFixtures() throws Exception {
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

            MockHttpServletRequestBuilder requestBuilder = switch (id) {
                case "assist.auth.missing" ->
                        post("/api/assist/test/action")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"test\":\"data\"}");
                case "assist.auth.expired" ->
                        post("/api/assist/test/action")
                                .header("Authorization", "Bearer " + createToken(-60_000))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"test\":\"data\"}");
                case "assist.auth.invalid" ->
                        post("/api/assist/test/action")
                                .header("Authorization", "Bearer invalid.token.value")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"test\":\"data\"}");
                case "assist.malformedJson" ->
                        post("/api/assist/test/action")
                                .header("Authorization", "Bearer " + createToken(60_000))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"malformed\": json without close");
                case "assist.methodNotAllowed" ->
                        get("/api/assist/test/action")
                                .header("Authorization", "Bearer " + createToken(60_000));
                case "assist.route.notFound" ->
                        get("/api/assist/non-existent-path")
                                .header("Authorization", "Bearer " + createToken(60_000));
                case "assist.internal" ->
                        get("/api/assist/test/internal-error")
                                .header("Authorization", "Bearer " + createToken(60_000));
                default -> throw new IllegalArgumentException("Unknown assist scenario id: " + id);
            };

            MvcResult result = mockMvc.perform(requestBuilder).andReturn();
            int actualStatus = result.getResponse().getStatus();
            String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

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
                String allowHeader = result.getResponse().getHeader("Allow");
                assertThat(allowHeader)
                        .as("Allow header on 405 scenario " + id)
                        .isNotNull()
                        .contains("POST");
            }

            // 내부 스택이나 예외명 누출 검증
            assertThat(body)
                    .as("Internal exception leakage check for scenario " + id)
                    .doesNotContain("Exception")
                    .doesNotContain("at com.");
        }

        assertThat(assistScenariosCount).isEqualTo(7);
    }

    @Test
    void testErrorCodeEnumMatchesContractCodesFixture() {
        JsonNode codes = fixturesJson.path("codes");
        assertThat(codes.isObject()).isTrue();

        java.util.List<String> fixtureCodeNames = new java.util.ArrayList<>();
        codes.fieldNames().forEachRemaining(fixtureCodeNames::add);
        java.util.Collections.sort(fixtureCodeNames);

        java.util.List<String> enumNames = java.util.Arrays.stream(ErrorCode.values())
                .map(Enum::name)
                .sorted()
                .toList();

        assertThat(enumNames)
                .as("ErrorCode enum values must match contract fixture codes exactly")
                .isEqualTo(fixtureCodeNames);

        for (ErrorCode code : ErrorCode.values()) {
            JsonNode codeNode = codes.path(code.name());
            assertThat(code.getStatus()).isEqualTo(codeNode.path("http").asInt());
            assertThat(code.getDefaultMessage()).isEqualTo(codeNode.path("message").asText());
            assertThat(code.isFixedMessage()).isEqualTo(codeNode.path("fixedMessage").asBoolean());
        }
    }
}
