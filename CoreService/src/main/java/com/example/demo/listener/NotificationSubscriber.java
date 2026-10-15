package com.example.demo.listener;

import com.example.demo.dto.Market.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;


@RequiredArgsConstructor
@Component
@Slf4j
public class NotificationSubscriber implements MessageListener {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            var body = objectMapper.readTree(message.getBody());
            if (body == null || !body.has("receiverEmail") || !body.has("message")
                    || !body.hasNonNull("chatroomId") || !body.hasNonNull("productId")) {
                log.warn("필수 필드가 누락된 알림 메시지");
                return;
            }
            NotificationMessage notification = objectMapper.treeToValue(body, NotificationMessage.class);
            messagingTemplate.convertAndSend("/topic/user/" + notification.getReceiverEmail(), notification);
        } catch (Exception e) {
            log.error("Redis 알림 처리 중 오류 발생", e);
        }
    }
}
