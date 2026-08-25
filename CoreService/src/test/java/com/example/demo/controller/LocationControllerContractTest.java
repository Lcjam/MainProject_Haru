package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Location;
import com.example.demo.service.LocationService;
import com.example.demo.service.NotificationService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LocationController 의 현행 HTTP 계약을 고정한다 (Phase 1a).
 * {@code @MessageMapping("/location/{chatroomId}")} (handleLocationUpdate) 는 MockMvc 로 태울 수 없으므로
 * 이 클래스에서 제외한다 — 별도 단위에서 LocationService 의 Mockito 단위 테스트로 다룬다.
 */
@DisplayName("LocationController contract (HTTP 엔드포인트만)")
class LocationControllerContractTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private final LocationService locationService = mock(LocationService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LocationController(messagingTemplate, tokenUtils, locationService,
                        notificationService, userMapper))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- GET /api/core/location/rooms/{chatroomId}/recent (getRecentLocations) ----

    @Test
    @DisplayName("getRecentLocations: 이메일 null이면 401")
    void getRecentLocations_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/recent", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.data").value("인증되지 않은 요청입니다."));
    }

    @Test
    @DisplayName("getRecentLocations: 정상 조회는 200 + success")
    void getRecentLocations_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(locationService.getRecentLocations(1)).willReturn(List.of());

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/recent", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("getRecentLocations: 예외 발생 시 500 + 고정 메시지")
    void getRecentLocations_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(locationService.getRecentLocations(anyInt())).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/recent", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }

    // ---- GET /api/core/location/rooms/{chatroomId}/users/{email}/last (getLastLocation) ----

    @Test
    @DisplayName("getLastLocation: 요청자 이메일 null이면 401")
    void getLastLocation_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/users/{email}/last", 1, "other@haru.com")
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.data").value("인증되지 않은 요청입니다."));
    }

    @Test
    @DisplayName("getLastLocation: 정상 조회는 200 + success")
    void getLastLocation_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        Location location = new Location();
        location.setChatroomId(1);
        location.setEmail("other@haru.com");
        given(locationService.getLastLocation(1, "other@haru.com")).willReturn(location);

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/users/{email}/last", 1, "other@haru.com")
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("getLastLocation: 예외 발생 시 500 + 고정 메시지")
    void getLastLocation_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(locationService.getLastLocation(anyInt(), anyString())).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/users/{email}/last", 1, "other@haru.com")
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }
}
