package com.example.demo.controller;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.security.oauth2.CustomOAuth2User;
import com.example.demo.security.oauth2.OAuth2LoginCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OAuth2ControllerTest {
    private OAuth2LoginCodeService codeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
        when(tokenProvider.createToken(any(), any(), any())).thenReturn("jwt-value");
        codeService = new OAuth2LoginCodeService(tokenProvider);
        mockMvc = MockMvcBuilders.standaloneSetup(new OAuth2Controller(codeService)).build();
    }

    @Test
    void exchangesTheCodeOnceAndRejectsAReplayOrAnotherBrowserSession() throws Exception {
        var session = new MockHttpSession();
        String code = issue(session);

        mockMvc.perform(post("/api/core/auth/oauth2/exchange")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-value"));

        mockMvc.perform(post("/api/core/auth/oauth2/exchange")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID"));

        String secondCode = issue(session);
        mockMvc.perform(post("/api/core/auth/oauth2/exchange")
                        .session(new MockHttpSession())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"" + secondCode + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String issue(MockHttpSession session) {
        var request = new MockHttpServletRequest();
        request.setSession(session);
        return codeService.issue(request, new CustomOAuth2User(7, "user@example.com", "Haru",
                List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("email", "user@example.com"), "email"));
    }
}
