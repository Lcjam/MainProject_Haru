package com.example.demo.listener;

import com.example.demo.dto.Market.NotificationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

class NotificationSubscriberTest {
    private final SimpMessagingTemplate websocket = mock(SimpMessagingTemplate.class);
    private final NotificationSubscriber subscriber = new NotificationSubscriber(websocket, new ObjectMapper());

    @Test
    void relaysThePayloadProducedByTheConfiguredRedisSerializer() {
        var notification = new NotificationMessage("user@example.test", "알림", "PRODUCT_REQUEST", 42, 99L);
        byte[] payload = new Jackson2JsonRedisSerializer<>(Object.class).serialize(notification);

        subscriber.onMessage(new org.springframework.data.redis.connection.DefaultMessage("notification".getBytes(StandardCharsets.UTF_8), payload), null);

        verify(websocket).convertAndSend("/topic/user/user@example.test", notification);
    }

    @Test
    void ignoresIncompleteAndMalformedPayloads() {
        for (String body : new String[]{"null", "invalid-json", "{}",
                "{\"receiverEmail\":\"user@example.test\",\"message\":\"hello\"}",
                "{\"receiverEmail\":\"user@example.test\",\"message\":\"hello\",\"chatroomId\":\"bad\",\"productId\":1}"}) {
            subscriber.onMessage(new org.springframework.data.redis.connection.DefaultMessage(new byte[0], body.getBytes(StandardCharsets.UTF_8)), null);
        }
        verifyNoInteractions(websocket);
    }
}
