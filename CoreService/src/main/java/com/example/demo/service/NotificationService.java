package com.example.demo.service;

import com.example.demo.dto.Market.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor  // 자동으로 생성자 생성
@Slf4j  // 로깅 추가
public class NotificationService {
    /** 상품 요청 발생 시 이메일 및 푸시 알림 전송 **/
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    // 알림용 Topic을 명확하게 지정
    private final @Qualifier("notificationChannelTopic") ChannelTopic notificationChannelTopic;

    public void sendNotification(String receiverEmail, String message, String type, Integer chatroomId, Long productId) {
        try {
            log.info("알림 전송 시도: 수신자={}, 메시지={}, 타입={}, 채팅방ID={}, 상품ID={}", receiverEmail, message, type, chatroomId, productId);
            
            NotificationMessage notification = new NotificationMessage(receiverEmail, message, type, chatroomId, productId);

            // WebSocket을 통해 클라이언트에게 즉시 전송 (먼저 처리)
            String destination = "/topic/user/" + receiverEmail;
            messagingTemplate.convertAndSend(destination, notification);
            log.info("WebSocket으로 알림 전송 완료: destination={}", destination);
            
            // 세션 및 연결된 클라이언트 수 확인 (디버깅용)
            log.info("messagingTemplate: {}", messagingTemplate);
            
            try {
                // Redis Pub/Sub으로 알림 전송 (옵션)
                redisTemplate.convertAndSend(notificationChannelTopic.getTopic(), notification);
                log.info("Redis에 알림 발행 완료: topic={}", notificationChannelTopic.getTopic());
            } catch (Exception redisEx) {
                // Redis 문제가 있더라도 WebSocket 전송은 계속 진행
                log.warn("Redis 발행 중 오류 발생 (WebSocket 전송은 완료됨): {}", redisEx.getMessage());
            }
        } catch (Exception e) {
            log.error("알림 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }
}

