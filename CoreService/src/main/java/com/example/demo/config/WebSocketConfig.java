package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.ChannelInterceptor;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    public WebSocketConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public ThreadPoolTaskScheduler messageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("websocket-heartbeat-thread-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트에서 구독할 주제 경로 설정
        registry.enableSimpleBroker(
            "/topic"     // 일반 토픽만 사용
        )
        .setTaskScheduler(messageBrokerTaskScheduler())
        .setHeartbeatValue(new long[] {10000, 10000});
        
        // 클라이언트에서 서버로 메시지를 전송할 때 사용할 접두사
        registry.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 연결 시 토큰 검증
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        String token = authorization.get(0).replace("Bearer ", "");
                        
                        try {
                            // 토큰 검증 및 이메일 추출
                            if (jwtTokenProvider.validateToken(token)) {
                                String email = jwtTokenProvider.getUsername(token);
                                
                                // Principal 설정 (이후 메시지 처리에서 사용됨)
                                accessor.setUser(new Principal() {
                                    @Override
                                    public String getName() {
                                        return email;
                                    }
                                });
                                
                                log.info("WebSocket 연결 인증 성공: email={}", email);
                            }
                        } catch (Exception e) {
                            log.error("WebSocket 연결 인증 실패: {}", e.getMessage());
                            return null; // 연결 거부
                        }
                    } else {
                        log.error("WebSocket 연결 인증 헤더 없음");
                        return null; // 연결 거부
                    }
                }
                return message;
            }
        });
    }
    
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // 아웃바운드 채널 설정 (서버 -> 클라이언트)
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(10)
                .queueCapacity(25);
    }
    
    @Bean
    public MappingJackson2MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        converter.setObjectMapper(objectMapper);
        return converter;
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(messageConverter());
        return false;
    }
}

