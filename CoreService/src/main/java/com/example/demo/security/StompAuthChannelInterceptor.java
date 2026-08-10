package com.example.demo.security;

import com.example.demo.util.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * STOMP 인바운드 채널 인증 인터셉터.
 *
 * <p>게이트웨이는 {@code /ws/**} 를 JWT 필터에서 의도적으로 제외하므로,
 * WebSocket 경로의 인증 방어선은 이 클래스가 유일하다.
 *
 * <p>WebSocketConfig 의 익명 ChannelInterceptor 에서 추출했다(익명 클래스는 단위 테스트 불가).
 * 이 커밋은 순수 이동이며 동작은 이전과 동일하다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenUtils tokenUtils;

    public StompAuthChannelInterceptor(JwtTokenProvider jwtTokenProvider, TokenUtils tokenUtils) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenUtils = tokenUtils;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 연결 시 토큰 검증
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                String token = tokenUtils.extractTokenWithoutBearer(authorization.get(0));

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
}
