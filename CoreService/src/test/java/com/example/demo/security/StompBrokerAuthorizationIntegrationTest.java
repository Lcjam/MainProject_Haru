package com.example.demo.security;

import com.example.demo.config.WebSocketConfig;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = StompBrokerAuthorizationIntegrationTest.TestApplication.class)
@DisplayName("실제 STOMP broker 권한 경로")
class StompBrokerAuthorizationIntegrationTest {

    private static final String EMAIL = "member@haru.com";

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtTokenBlacklistService blacklistService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ChatRoomMapper chatRoomMapper;

    @Autowired
    private SimpUserRegistry userRegistry;

    @Autowired
    private ProbeController probeController;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketStompClient client;
    private StompSession session;

    @BeforeEach
    void connect() throws Exception {
        reset(jwtTokenProvider, blacklistService, userMapper, chatRoomMapper);
        probeController.receivedChatroomIds.clear();
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getUsername("valid-token")).willReturn(EMAIL);
        given(blacklistService.isBlacklisted("valid-token")).willReturn(false);
        given(userMapper.findByEmail(EMAIL)).willReturn(
                User.builder().email(EMAIL).accountStatus("Active").build());
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(new ChatRoom());

        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");
        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.setOrigin("http://localhost:3000");
        session = client.connectAsync(
                        "ws://localhost:" + port + "/ws",
                        handshakeHeaders,
                        connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnect() {
        if (client != null) {
            client.stop();
        }
    }

    @Test
    @DisplayName("멤버 프레임만 broker 구독과 message handler에 도달한다")
    void onlyMemberFramesReachBrokerAndHandler() throws Exception {
        session.subscribe("/topic/room.7", emptyFrameHandler());
        session.subscribe("/topic/room.8", emptyFrameHandler());

        assertTrue(waitUntil(() -> hasSubscription("/topic/room.7"), 2_000));
        assertFalse(waitUntil(() -> hasSubscription("/topic/room.8"), 500));

        session.send("/app/chat/send", Map.of("chatroomId", 7));
        assertEquals(7, probeController.receivedChatroomIds.poll(2, TimeUnit.SECONDS));

        session.send("/app/chat/send", Map.of("chatroomId", 8));
        assertNull(probeController.receivedChatroomIds.poll(500, TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("열린 구독은 outbound 직전에 로그아웃·만료·탈퇴·방 탈퇴를 다시 검사한다")
    void openedSubscription_stopsReceivingAfterAccessIsRevoked() throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/room.7", frameHandler(received));
        assertTrue(waitUntil(() -> hasSubscription("/topic/room.7"), 2_000));

        messagingTemplate.convertAndSend("/topic/room.7", "allowed");
        assertEquals("allowed", received.poll(2, TimeUnit.SECONDS));

        given(blacklistService.isBlacklisted("valid-token")).willReturn(true);
        assertOutboundBlocked(received, "blacklisted");

        given(blacklistService.isBlacklisted("valid-token")).willReturn(false);
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(false);
        assertOutboundBlocked(received, "expired");

        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(userMapper.findByEmail(EMAIL)).willReturn(
                User.builder().email(EMAIL).accountStatus("Withdrawal").build());
        assertOutboundBlocked(received, "withdrawn");

        given(userMapper.findByEmail(EMAIL)).willReturn(
                User.builder().email(EMAIL).accountStatus("Active").build());
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(null);
        assertOutboundBlocked(received, "removed-member");
    }

    @Test
    @DisplayName("허용하지 않은 Origin은 실제 WebSocket handshake에서 거부한다")
    void disallowedOrigin_isRejectedAtHandshake() {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setOrigin("https://attacker.example");
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer valid-token");
        WebSocketStompClient rejectedClient = new WebSocketStompClient(new StandardWebSocketClient());

        try {
            assertThrows(ExecutionException.class, () -> rejectedClient.connectAsync(
                            "ws://localhost:" + port + "/ws",
                            headers,
                            connectHeaders,
                            new StompSessionHandlerAdapter() {})
                    .get(5, TimeUnit.SECONDS));
        } finally {
            rejectedClient.stop();
        }
    }

    private void assertOutboundBlocked(BlockingQueue<String> received, String payload) throws InterruptedException {
        messagingTemplate.convertAndSend("/topic/room.7", payload);
        assertNull(received.poll(500, TimeUnit.MILLISECONDS));
    }

    private boolean hasSubscription(String destination) {
        SimpUser user = userRegistry.getUser(EMAIL);
        return user != null && user.getSessions().stream()
                .flatMap(stompSession -> stompSession.getSubscriptions().stream())
                .anyMatch(subscription -> destination.equals(subscription.getDestination()));
    }

    private boolean waitUntil(BooleanSupplier condition, long timeoutMillis) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        do {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(20);
        } while (System.nanoTime() < deadline);
        return condition.getAsBoolean();
    }

    private StompFrameHandler emptyFrameHandler() {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
            }
        };
    }

    private StompFrameHandler frameHandler(BlockingQueue<String> received) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((String) payload);
            }
        };
    }

    @SpringBootConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
            SecurityAutoConfiguration.class,
            OAuth2ClientAutoConfiguration.class
    })
    @Import({WebSocketConfig.class, TestBeans.class})
    static class TestApplication {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {
        @Bean
        TokenUtils tokenUtils(
                JwtTokenProvider jwtTokenProvider,
                JwtTokenBlacklistService blacklistService) {
            return new TokenUtils(jwtTokenProvider, blacklistService);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        JwtTokenBlacklistService blacklistService() {
            return mock(JwtTokenBlacklistService.class);
        }

        @Bean
        UserMapper userMapper() {
            return mock(UserMapper.class);
        }

        @Bean
        ChatRoomMapper chatRoomMapper() {
            return mock(ChatRoomMapper.class);
        }

        @Bean
        StompAuthChannelInterceptor stompAuthChannelInterceptor(
                TokenUtils tokenUtils,
                UserMapper userMapper,
                ChatRoomMapper chatRoomMapper) {
            return new StompAuthChannelInterceptor(tokenUtils, userMapper, chatRoomMapper);
        }

        @Bean
        ProbeController probeController() {
            return new ProbeController();
        }
    }

    @Controller
    static class ProbeController {
        private final BlockingQueue<Integer> receivedChatroomIds = new LinkedBlockingQueue<>();

        @MessageMapping("/chat/send")
        void receive(Map<String, Integer> payload) {
            receivedChatroomIds.add(payload.get("chatroomId"));
        }
    }
}
