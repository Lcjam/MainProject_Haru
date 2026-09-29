package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.model.Location;
import com.example.demo.model.User;
import com.example.demo.service.LocationService;
import com.example.demo.service.NotificationService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/core/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final TokenUtils tokenUtils;
    private final LocationService locationService;
    private final NotificationService notificationService;


    /**
     * WebSocket을 통한 위치 업데이트 처리
     * /app/location/{chatroomId} 로 전송된 메시지를 처리합니다.
     */
    @MessageMapping("/location/{chatroomId}")
    public void handleLocationUpdate(
            @DestinationVariable Integer chatroomId,
            @Payload Location location,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        if (principal == null) {
            log.error("위치 업데이트 처리 오류: 인증되지 않은 사용자");
            return;
        }

        String senderEmail = principal.getName();
        log.info("위치 업데이트 수신: chatroomId={}, senderEmail={}, location={}", 
                chatroomId, senderEmail, location);
        

        try {
            if (chatroomId == null || location == null || !chatroomId.equals(location.getChatroomId())) {
                throw new IllegalArgumentException("목적지와 위치 정보의 채팅방이 일치하지 않습니다.");
            }
            // 위치 정보 저장
            location.setEmail(senderEmail);
            locationService.saveLocation(location);

            // 위치 정보를 채팅방의 모든 구독자에게 브로드캐스트
            messagingTemplate.convertAndSend(
                "/topic/location." + chatroomId,
                location
            );
            User sender = locationService.findUserByEmail(senderEmail);
            String sendNickname = sender.getNickname();

            // 알림 추가 (Redis를 통한 알림)
            String message = String.format("%s님의 위치 정보가 업데이트되었습니다!", sendNickname);
            notificationService.sendNotification(
                senderEmail,
                message,
                "LOCATION_SHARE",
                chatroomId,
                0L
            );

            log.info("위치 정보 발행 완료: chatroomId={}, email={}", 
                    chatroomId, senderEmail);
        } catch (Exception e) {
            log.error("위치 정보 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * REST API를 통한 최근 위치 조회
     * JWT 토큰으로 인증된 사용자의 HTTP 요청을 처리합니다.
     */
    @GetMapping("/rooms/{chatroomId}/recent")
    public ResponseEntity<ApiResponse<?>> getRecentLocations(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Integer chatroomId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        var locations = locationService.getRecentLocations(chatroomId, email);
        return ResponseEntity.ok(ApiResponse.success(locations));
    }

    /**
     * 특정 사용자의 마지막 위치 조회 상대방의 위치 초기값으로 설정하기 좋을듯
     */
    @GetMapping("/rooms/{chatroomId}/users/{email}/last")
    public ResponseEntity<ApiResponse<?>> getLastLocation(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Integer chatroomId,
            @PathVariable String email) {
        
        String requestEmail = tokenUtils.getEmailFromAuthHeader(token);
        
        if (requestEmail == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        var location = locationService.getLastLocation(chatroomId, email, requestEmail);
        return ResponseEntity.ok(ApiResponse.success(location));
    }
}
