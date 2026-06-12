package com.example.demo.listener;

import com.example.demo.dto.Market.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationSubscriber implements MessageListener {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        log.info("Redis 알림 수신 시작: pattern={}", new String(pattern));
        
        try {
            // 문자열로 원본 메시지 출력
            String messageStr = new String(message.getBody());
            log.info("원본 메시지: {}", messageStr);
            
            // Jackson ObjectMapper로 메시지 파싱
            Map<String, String> map = objectMapper.readValue(message.getBody(), Map.class);
            
            if (map != null && map.containsKey("receiverEmail") && map.containsKey("message")) {
                // Map에서 NotificationMessage 생성
                NotificationMessage notification = new NotificationMessage(
                    map.get("receiverEmail"),
                    map.get("message"),
                    map.get("type"),
                    Integer.parseInt(map.get("chatroomId")),
                    Long.parseLong(map.get("productId"))
                );
                
                log.info("Redis에서 알림 수신 성공: 수신자={}, 메시지={}", 
                        notification.getReceiverEmail(), notification.getMessage());
                
                // WebSocket을 통해 해당 사용자에게 알림 전송
                String destination = "/topic/user/" + notification.getReceiverEmail();
                messagingTemplate.convertAndSend(destination, notification);
                log.info("WebSocket으로 알림 전송 완료: destination={}, message={}", 
                        destination, notification.getMessage());
            } else {
                log.warn("필수 필드가 누락된 알림 메시지: {}", map);
            }
        } catch (Exception e) {
            log.error("Redis 알림 처리 중 오류 발생: {}", e.getMessage(), e);
            log.error("문제가 발생한 메시지: {}", new String(message.getBody()));
            log.error("문제가 발생한 패턴: {}", new String(pattern));
        }
    }
}
