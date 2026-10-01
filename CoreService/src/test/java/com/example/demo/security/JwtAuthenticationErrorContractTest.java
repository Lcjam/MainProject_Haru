package com.example.demo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JwtAuthenticationErrorContractTest {
    private static final String SECRET = "test-secret-for-core-auth-contract";
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void missingTokenHasRequiredCode() throws Exception {
        var response = new MockHttpServletResponse();
        new JwtAuthenticationEntryPoint().commence(new MockHttpServletRequest(), response, null);
        assertError(response, "AUTH_REQUIRED");
    }

    @Test
    void expiredAndForgedTokensHaveDistinctCodes() throws Exception {
        String expired = new JwtTokenProvider(SECRET, -60_000)
                .createToken(1, "tester@example.com", List.of("ROLE_USER"));
        String valid = new JwtTokenProvider(SECRET, 60_000)
                .createToken(1, "tester@example.com", List.of("ROLE_USER"));
        assertError(filter(expired), "AUTH_EXPIRED");
        assertError(filter(valid + "tampered"), "AUTH_INVALID");
    }

    @Test
    void malformedAuthorizationHeaderIsInvalidRatherThanMissing() throws Exception {
        assertError(filterAuthorization("Basic abc"), "AUTH_INVALID");
        assertError(filterAuthorization("Bearer"), "AUTH_INVALID");
        assertError(filterAuthorization("Bearer "), "AUTH_INVALID");
    }

    @Test
    void fakeWebsocketUpgradeCannotSkipRestTokenValidation() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/core/chat/rooms/1");
        request.addHeader("Upgrade", "websocket");
        request.addHeader("Authorization", "Bearer forged");
        var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(new JwtTokenProvider(SECRET, 60_000),
                mock(JwtTokenBlacklistService.class)).doFilter(request, response, new MockFilterChain());
        assertError(response, "AUTH_INVALID");
    }

    private MockHttpServletResponse filter(String token) throws Exception {
        return filterAuthorization("Bearer " + token);
    }

    private MockHttpServletResponse filterAuthorization(String authorization) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/core/profiles/me");
        request.addHeader("Authorization", authorization);
        var response = new MockHttpServletResponse();
        new JwtAuthenticationFilter(new JwtTokenProvider(SECRET, 60_000),
                mock(JwtTokenBlacklistService.class)).doFilter(request, response, new MockFilterChain());
        return response;
    }

    private void assertError(MockHttpServletResponse response, String code) throws Exception {
        assertEquals(401, response.getStatus());
        var body = mapper.readTree(response.getContentAsString());
        assertEquals("error", body.get("status").asText());
        assertEquals(code, body.get("code").asText());
        assertTrue(body.get("message").asText().length() > 0);
    }
}
