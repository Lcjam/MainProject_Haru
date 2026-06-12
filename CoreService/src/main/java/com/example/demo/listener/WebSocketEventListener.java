package com.example.demo.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = getUsernameFromEvent(headerAccessor);
        if (username != null) {
            log.info("User Connected: {}", username);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = getUsernameFromEvent(headerAccessor);
        
        if (username != null) {
            log.info("User Disconnected: {}", username);
            
            // 여기서 필요하다면 사용자 연결 해제 시 추가적인 작업 수행 가능
            // 예: 채팅방에 사용자 퇴장 메시지 전송, 온라인 상태 업데이트 등
        }
    }
    
    private String getUsernameFromEvent(StompHeaderAccessor headerAccessor) {
        if (headerAccessor.getUser() != null) {
            return headerAccessor.getUser().getName(); // JWT 인증 시 이메일이 설정됨
        }
        return null;
    }
}
