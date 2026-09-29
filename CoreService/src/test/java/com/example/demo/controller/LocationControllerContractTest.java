package com.example.demo.controller;

import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Location;
import com.example.demo.model.User;
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
import java.security.Principal;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LocationController 의 HTTP 응답과 STOMP 핸들러 발행 경계를 고정한다.
 */
@DisplayName("LocationController HTTP/STOMP contract")
class LocationControllerContractTest {

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private final LocationService locationService = mock(LocationService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new LocationController(messagingTemplate, tokenUtils, locationService,
                        notificationService))
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
    @DisplayName("위치 REST는 Authorization이 없으면 401을 반환한다")
    void locationReads_missingAuthorizationReturn401() throws Exception {
        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/recent", 1))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/users/{email}/last", 1,
                        "other@haru.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("getRecentLocations: 정상 조회는 200 + success")
    void getRecentLocations_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(locationService.getRecentLocations(1, "user@haru.com")).willReturn(List.of());

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
        given(locationService.getRecentLocations(anyInt(), anyString())).willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/recent", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("getRecentLocations: 방 멤버가 아니면 자원 존재를 숨긴 404")
    void getRecentLocations_nonMember_returns404() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("outsider@haru.com");
        given(locationService.getRecentLocations(1, "outsider@haru.com"))
                .willThrow(new NotFoundException("채팅방을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/recent", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.data").value("채팅방을 찾을 수 없습니다."));
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
        given(locationService.getLastLocation(1, "other@haru.com", "user@haru.com")).willReturn(location);

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
        given(locationService.getLastLocation(anyInt(), anyString(), anyString()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/users/{email}/last", 1, "other@haru.com")
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }

    @Test
    @DisplayName("getLastLocation: 요청자나 대상자가 같은 방 멤버가 아니면 404")
    void getLastLocation_nonMember_returns404() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(locationService.getLastLocation(1, "outsider@haru.com", "user@haru.com"))
                .willThrow(new NotFoundException("채팅방을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/core/location/rooms/{chatroomId}/users/{email}/last", 1,
                        "outsider@haru.com").header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"));
    }

    @Test
    @DisplayName("handleLocationUpdate: 목적지와 payload 방이 같을 때 저장 후 해당 방에 발행한다")
    void handleLocationUpdate_matchingRoom_savesAndPublishes() {
        Location location = location(7, 37.0, 127.0);
        given(locationService.findUserByEmail("sender@haru.com"))
                .willReturn(User.builder().email("sender@haru.com").nickname("sender").build());

        controller().handleLocationUpdate(7, location, null, principal("sender@haru.com"));

        verify(locationService).saveLocation(location);
        verify(messagingTemplate).convertAndSend("/topic/location.7", location);
    }

    @Test
    @DisplayName("handleLocationUpdate: 목적지와 payload 방이 다르면 저장·발행하지 않는다")
    void handleLocationUpdate_mismatchedRoom_rejected() {
        controller().handleLocationUpdate(7, location(8, 37.0, 127.0), null,
                principal("sender@haru.com"));

        verifyNoInteractions(locationService, messagingTemplate, notificationService);
    }

    @Test
    @DisplayName("handleLocationUpdate: 좌표나 멤버십 검증 실패 후에는 발행하지 않는다")
    void handleLocationUpdate_serviceRejects_doesNotPublish() {
        Location location = location(7, Double.NaN, 127.0);
        willThrow(new IllegalArgumentException("유효한 위도와 경도가 필요합니다."))
                .given(locationService).saveLocation(location);

        controller().handleLocationUpdate(7, location, null, principal("sender@haru.com"));

        verifyNoInteractions(messagingTemplate, notificationService);
    }

    private LocationController controller() {
        return new LocationController(messagingTemplate, tokenUtils, locationService, notificationService);
    }

    private static Principal principal(String email) {
        return () -> email;
    }

    private static Location location(int chatroomId, double latitude, double longitude) {
        Location location = new Location();
        location.setChatroomId(chatroomId);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }
}
