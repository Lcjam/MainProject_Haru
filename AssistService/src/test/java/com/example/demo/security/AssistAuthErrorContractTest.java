package com.example.demo.security;

import com.example.demo.controller.LlamaController;
import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.serviceimpl.LlamaServiceImpl;
import com.example.demo.util.TokenUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistAuthErrorContractTest {
    private static final String SECRET = "assist-contract-test-secret-value-long-enough";
    private final ChatMessageDAO dao = mock(ChatMessageDAO.class);
    private final LlamaServiceImpl service = mock(LlamaServiceImpl.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new LlamaController(service, dao, new TokenUtils(SECRET))).build();

    @Test
    void missingExpiredAndForgedTokensReturnMachineCodes() throws Exception {
        assertRejected(null, "AUTH_REQUIRED");
        assertRejected("Bearer " + token(-60_000), "AUTH_EXPIRED");
        assertRejected("Bearer " + token(60_000) + "tampered", "AUTH_INVALID");
        assertRejected("Basic abc", "AUTH_INVALID");
        assertRejected("Bearer", "AUTH_INVALID");
        assertRejected("Bearer ", "AUTH_INVALID");
        verifyNoInteractions(dao, service);
    }

    @Test
    void validTokenReachesOwnHistory() throws Exception {
        when(dao.getMessagesBySessionId("a@haru.test")).thenReturn(List.of());
        when(service.chat("hello", "a@haru.test", List.of())).thenReturn("reply");
        mvc.perform(post("/api/assist/tinylamanaver/chat")
                        .header("Authorization", "Bearer " + token(60_000))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.response").value("reply"));
    }

    private void assertRejected(String authorization, String code) throws Exception {
        var request = post("/api/assist/tinylamanaver/chat")
                .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"hello\"}");
        if (authorization != null) request.header("Authorization", authorization);
        mvc.perform(request)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private String token(long lifetimeMillis) {
        byte[] key = Base64.getEncoder().encodeToString(SECRET.getBytes(StandardCharsets.UTF_8))
                .getBytes(StandardCharsets.UTF_8);
        return Jwts.builder().setSubject("a@haru.test")
                .setExpiration(new Date(System.currentTimeMillis() + lifetimeMillis))
                .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256).compact();
    }
}
