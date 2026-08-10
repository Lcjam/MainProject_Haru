package com.example.demo.security;

import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * S1(STOMP CONNECT 인증 우회) 회귀 방지.
 *
 * <p>수정 전 결함: {@code validateToken()} 이 위조 토큰에 예외가 아니라 {@code false} 를
 * 돌려주는데 인터셉터가 if 블록만 건너뛰고 {@code return message} 로 진행해,
 * 서명이 가짜인 토큰으로도 CONNECT 가 수락됐다. 게이트웨이는 {@code /ws/**} 를 JWT 필터에서
 * 제외하므로 이 인터셉터가 유일한 방어선이다.
 *
 * <p>{@code preSend} 가 {@code null} 을 돌려주면 메시지가 파기되어 연결이 거부된다.
 */
@DisplayName("StompAuthChannelInterceptor CONNECT 인증")
class StompAuthChannelInterceptorTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private final MessageChannel channel = mock(MessageChannel.class);

    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(jwtTokenProvider, tokenUtils);

    private Message<byte[]> connectMessage(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("유효 토큰: 연결 수락 + Principal 에 이메일 설정")
    void validToken_acceptsAndSetsPrincipal() {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("valid-token");
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getUsername("valid-token")).willReturn("user@haru.com");

        Message<byte[]> message = connectMessage("Bearer valid-token");
        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result, "유효 토큰은 통과해야 한다");

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(accessor);
        assertNotNull(accessor.getUser(), "Principal 이 설정돼야 한다");
        assertEquals("user@haru.com", accessor.getUser().getName());
    }

    @Test
    @DisplayName("위조 토큰(validateToken=false): 연결 거부 — S1 회귀 방지")
    void forgedToken_rejected() {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("forged-token");
        // 예외가 아니라 false 를 돌려주는 경로가 바로 S1 의 진입점이었다.
        given(jwtTokenProvider.validateToken("forged-token")).willReturn(false);

        Message<?> result = interceptor.preSend(connectMessage("Bearer forged-token"), channel);

        assertNull(result, "위조 토큰은 연결이 거부돼야 한다");
    }

    @Test
    @DisplayName("Authorization 헤더 없음: 연결 거부")
    void missingHeader_rejected() {
        Message<?> result = interceptor.preSend(connectMessage(null), channel);

        assertNull(result, "인증 헤더가 없으면 연결이 거부돼야 한다");
    }

    @Test
    @DisplayName("검증 중 예외: 연결 거부")
    void tokenValidationThrows_rejected() {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("boom");
        given(jwtTokenProvider.validateToken("boom")).willThrow(new RuntimeException("parse error"));

        Message<?> result = interceptor.preSend(connectMessage("Bearer boom"), channel);

        assertNull(result, "검증 중 예외가 나면 연결이 거부돼야 한다");
    }

    @Test
    @DisplayName("CONNECT 이외 프레임은 인증 검사 대상이 아니다")
    void nonConnectFrame_passesThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message =
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertNotNull(interceptor.preSend(message, channel));
    }
}
