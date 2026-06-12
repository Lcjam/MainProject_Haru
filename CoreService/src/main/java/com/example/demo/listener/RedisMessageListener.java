package com.example.demo.listener;

import com.example.demo.model.chat.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageListener {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Redis 채널에서 메시지를 수신하여 WebSocket으로 전달
     */
    public void onMessage(String message) {
        try {
            // 메시지 역직렬화
            ChatMessage chatMessage = objectMapper.readValue(message, ChatMessage.class);
            
            // 채팅방 ID를 통해 WebSocket 주제로 메시지 발행
            String destination = "/topic/chat." + chatMessage.getChatroomId();
            messagingTemplate.convertAndSend(destination, chatMessage);
            
            log.info("Redis -> WebSocket 메시지 발행 성공: {} -> {}", chatMessage.getSenderEmail(), destination);
        } catch (Exception e) {
            log.error("Redis 메시지 수신 처리 중 오류 발생: {}", e.getMessage());
        }
    }
}
