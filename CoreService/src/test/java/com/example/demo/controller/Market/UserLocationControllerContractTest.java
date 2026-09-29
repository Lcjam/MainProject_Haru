package com.example.demo.controller.Market;

import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Market.UserLocation;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.Market.UserLocationService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserLocationControllerContractTest {
    private static final String EMAIL = "user@haru.com";

    private final UserLocationService service = mock(UserLocationService.class);
    private final JwtTokenProvider jwt = mock(JwtTokenProvider.class);
    private final JwtTokenBlacklistService blacklist = mock(JwtTokenBlacklistService.class);
    private final UserMapper users = mock(UserMapper.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TokenUtils tokenUtils = new TokenUtils(jwt, blacklist);
        mockMvc = MockMvcBuilders.standaloneSetup(new UserLocationController(service, tokenUtils, users)).build();
        given(jwt.validateToken("valid-token")).willReturn(true);
        given(jwt.getUsername("valid-token")).willReturn(EMAIL);
        given(users.findByEmail(EMAIL)).willReturn(User.builder().email(EMAIL).accountStatus("Active").build());
    }

    @Test
    void updateRequiresBearerToken() throws Exception {
        mockMvc.perform(post("/api/core/market/users/location")
                        .contentType("application/json")
                        .content("{\"latitude\":37.5,\"longitude\":127.0}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("토큰이 누락되었습니다. 인증이 필요합니다."));
    }

    @Test
    void updateRejectsInvalidBlacklistedAndInactiveUsers() throws Exception {
        assertUnauthorized("expired-token");

        given(jwt.validateToken("revoked-token")).willReturn(true);
        given(blacklist.isBlacklisted("revoked-token")).willReturn(true);
        assertUnauthorized("revoked-token");

        given(users.findByEmail(EMAIL)).willReturn(User.builder().email(EMAIL).accountStatus("Withdrawal").build());
        assertUnauthorized("valid-token");
    }

    @Test
    void updateUsesAuthenticatedEmailAndKeepsResponseContract() throws Exception {
        mockMvc.perform(post("/api/core/market/users/location")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("{\"locationName\":\"경계\",\"latitude\":-90,\"longitude\":180}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data").value("사용자 위치 업데이트 완료"));

        verify(service).updateUserLocation(any(UserLocation.class));
    }

    @Test
    void updateRejectsNullBody() throws Exception {
        mockMvc.perform(post("/api/core/market/users/location")
                        .header("Authorization", "Bearer valid-token")
                        .contentType("application/json")
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("위치 정보가 누락되었습니다."));
    }

    @Test
    void latestReadsOnlyAuthenticatedUsersOwnLocation() throws Exception {
        UserLocation location = UserLocation.builder().email(EMAIL).latitude(37.5).longitude(127.0).build();
        given(service.getUserLatestLocation(EMAIL)).willReturn(location);

        mockMvc.perform(get("/api/core/market/users/location/latest")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL));

        verify(service).getUserLatestLocation(EMAIL);
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(post("/api/core/market/users/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"latitude\":37.5,\"longitude\":127.0}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("유효하지 않은 토큰입니다."));
    }
}
