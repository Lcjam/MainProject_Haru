package com.example.demo.controller.Market;

import com.example.demo.config.OAuth2Config;
import com.example.demo.config.SecurityConfig;
import com.example.demo.security.JwtAuthenticationEntryPoint;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.Market.PaymentsService;
import com.example.demo.service.Market.TransactionsService;
import com.example.demo.util.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionsController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class,
        TransactionsSecurityChainTest.JwtTestConfig.class})
class TransactionsSecurityChainTest {
    private static final String SECRET =
            "d04-security-chain-secret-long-enough-for-hmac-signing";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TransactionsService transactionsService;

    @MockBean
    private PaymentsService paymentsService;

    @MockBean
    private JwtTokenBlacklistService blacklistService;

    @MockBean
    private OAuth2Config oAuth2Config;

    @Test
    void missingForgedAndExpiredTokensAreRejectedBeforeController() throws Exception {
        String forged = new JwtTokenProvider(
                "different-d04-security-chain-secret-long-enough", 60_000)
                .createToken(1, "attacker@haru.test", List.of("ROLE_USER"));
        String expired = new JwtTokenProvider(SECRET, -60_000)
                .createToken(1, "buyer@haru.test", List.of("ROLE_USER"));

        mockMvc.perform(get("/api/core/market/transactions/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(get("/api/core/market/transactions/user")
                        .header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
        mockMvc.perform(get("/api/core/market/transactions/user")
                        .header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));

        verifyNoInteractions(transactionsService, paymentsService);
    }

    @Test
    void validTokenPrincipalReachesControllerThroughSecurityChain() throws Exception {
        String valid = jwtTokenProvider.createToken(
                1, "buyer@haru.test", List.of("ROLE_USER"));
        given(transactionsService.getUserTransactions("buyer@haru.test"))
                .willReturn(ResponseEntity.ok(new BaseResponse<>(List.of())));

        mockMvc.perform(get("/api/core/market/transactions/user")
                        .header("Authorization", "Bearer " + valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(transactionsService).getUserTransactions("buyer@haru.test");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class JwtTestConfig {
        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return new JwtTokenProvider(SECRET, 60_000);
        }
    }
}
