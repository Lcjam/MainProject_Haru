package com.example.demo.security;

import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("STOMP 인증과 목적지 권한")
class StompAuthChannelInterceptorTest {

    private static final String EMAIL = "user@haru.com";

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final JwtTokenBlacklistService blacklistService = mock(JwtTokenBlacklistService.class);
    private final TokenUtils tokenUtils = new TokenUtils(jwtTokenProvider, blacklistService);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final StompAuthChannelInterceptor interceptor =
            new StompAuthChannelInterceptor(tokenUtils, userMapper, chatRoomMapper);
    private int sessionSequence;

    @BeforeEach
    void setUpActiveUser() {
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(jwtTokenProvider.getUsername("valid-token")).willReturn(EMAIL);
        given(blacklistService.isBlacklisted("valid-token")).willReturn(false);
        given(userMapper.findByEmail(EMAIL)).willReturn(user("Active"));
    }

    @Test
    @DisplayName("CONNECT: 유효한 토큰과 활성 계정은 Principal을 설정한다")
    void connect_activeUserAccepted() {
        String session = "session-1";

        Message<?> result = interceptor.preSend(connect("Bearer valid-token", session), channel);

        assertNotNull(result);
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertNotNull(accessor);
        assertNotNull(accessor.getUser());
        assertEquals(EMAIL, accessor.getUser().getName());
    }

    @Test
    @DisplayName("CONNECT: 헤더 누락, 폐기·만료 토큰, 탈퇴 계정을 거부한다")
    void connect_invalidAuthenticationRejected() {
        assertNull(interceptor.preSend(connect(null, "missing-header"), channel));

        given(jwtTokenProvider.validateToken("expired-token")).willReturn(false);
        assertNull(interceptor.preSend(connect("Bearer expired-token", "expired"), channel));

        given(jwtTokenProvider.validateToken("revoked-token")).willReturn(true);
        given(blacklistService.isBlacklisted("revoked-token")).willReturn(true);
        assertNull(interceptor.preSend(connect("Bearer revoked-token", "revoked"), channel));

        given(userMapper.findByEmail(EMAIL)).willReturn(user("Withdrawal"));
        assertNull(interceptor.preSend(connect("Bearer valid-token", "withdrawn"), channel));
    }

    @Test
    @DisplayName("SUBSCRIBE: 세 방 topic은 실제 방 멤버만 구독한다")
    void subscribe_roomTopicsRequireMembership() {
        String session = connectedSession();
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(new ChatRoom());

        assertNotNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/room.7", "", session), channel));
        assertNotNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/chat.7", "", session), channel));
        assertNotNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/location.7", "", session), channel));

        assertNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/room.8", "", session), channel));
    }

    @Test
    @DisplayName("SUBSCRIBE: 개인 topic은 자신의 이메일만 허용한다")
    void subscribe_personalTopicOnlyAllowsOwner() {
        String session = connectedSession();

        assertNotNull(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/" + EMAIL, "", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/other@haru.com", "", session), channel));
    }

    @Test
    @DisplayName("SUBSCRIBE: 미정의·비정규 목적지와 인증 없는 프레임을 거부한다")
    void subscribe_unknownAndUnauthenticatedRejected() {
        String session = connectedSession();

        assertNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/unknown.7", "", session), channel));
        assertNull(interceptor.preSend(frame(StompCommand.SUBSCRIBE, "/topic/room.07", "", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/room.7", "", "unauthenticated"), channel));

        assertNotNull(interceptor.preSend(frame(StompCommand.DISCONNECT, null, "", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/" + EMAIL, "", session), channel));
    }

    @Test
    @DisplayName("열린 세션: 로그아웃·만료 토큰과 탈퇴 계정을 다음 프레임에서 거부한다")
    void openedSession_rechecksTokenAndUser() {
        String revokedSession = connectedSession();
        given(blacklistService.isBlacklisted("valid-token")).willReturn(true);
        assertNull(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/" + EMAIL, "", revokedSession), channel));

        given(blacklistService.isBlacklisted("valid-token")).willReturn(false);
        String expiredSession = connectedSession();
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(false);
        assertNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/chat/send", "{\"chatroomId\":7}", expiredSession), channel));

        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(userMapper.findByEmail(EMAIL)).willReturn(user("Active"));
        String withdrawnSession = connectedSession();
        given(userMapper.findByEmail(EMAIL)).willReturn(user("Withdrawal"));
        assertNull(interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/" + EMAIL, "", withdrawnSession), channel));
    }

    @Test
    @DisplayName("outbound MESSAGE: 열린 구독도 토큰·계정·방 권한을 매번 다시 검사한다")
    void outboundMessage_rechecksSessionAndDestinationAccess() {
        String session = connectedSession();
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(new ChatRoom());

        assertNotNull(interceptor.preSendOutbound(
                frame(StompCommand.MESSAGE, "/topic/room.7", "message", session)));
        assertNotNull(interceptor.preSendOutbound(
                frame(StompCommand.MESSAGE, "/topic/user/" + EMAIL, "message", session)));

        given(blacklistService.isBlacklisted("valid-token")).willReturn(true);
        assertNull(interceptor.preSendOutbound(
                frame(StompCommand.MESSAGE, "/topic/room.7", "message", session)));

        given(blacklistService.isBlacklisted("valid-token")).willReturn(false);
        given(jwtTokenProvider.validateToken("valid-token")).willReturn(false);
        assertNull(interceptor.preSendOutbound(
                frame(StompCommand.MESSAGE, "/topic/room.7", "message", session)));

        given(jwtTokenProvider.validateToken("valid-token")).willReturn(true);
        given(userMapper.findByEmail(EMAIL)).willReturn(user("Withdrawal"));
        assertNull(interceptor.preSendOutbound(
                frame(StompCommand.MESSAGE, "/topic/room.7", "message", session)));

        given(userMapper.findByEmail(EMAIL)).willReturn(user("Active"));
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(null);
        assertNull(interceptor.preSendOutbound(
                frame(StompCommand.MESSAGE, "/topic/room.7", "message", session)));
    }

    @Test
    @DisplayName("SEND chat: payload 방 ID의 실제 멤버만 허용한다")
    void sendChat_requiresPayloadRoomMembership() {
        String session = connectedSession();
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(new ChatRoom());

        assertNotNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/chat/send", "{\"chatroomId\":7}", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/chat/send", "{\"chatroomId\":8}", session), channel));
    }

    @Test
    @DisplayName("SEND location: 목적지와 payload 방 ID가 같고 멤버일 때만 허용한다")
    void sendLocation_requiresMatchingRoomAndMembership() {
        String session = connectedSession();
        given(chatRoomMapper.findChatRoomById(7, EMAIL)).willReturn(new ChatRoom());

        assertNotNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/location/7", "{\"chatroomId\":7}", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/location/8", "{\"chatroomId\":7}", session), channel));
    }

    @Test
    @DisplayName("SEND: 미정의 목적지와 잘못된 payload를 거부한다")
    void send_unknownDestinationAndMalformedPayloadRejected() {
        String session = connectedSession();

        assertNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/unknown", "{\"chatroomId\":7}", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/chat/send", "{\"chatroomId\":\"7\"}", session), channel));
        assertNull(interceptor.preSend(
                frame(StompCommand.SEND, "/app/chat/send", "not-json", session), channel));
    }

    private String connectedSession() {
        String session = "session-" + ++sessionSequence;
        assertNotNull(interceptor.preSend(connect("Bearer valid-token", session), channel));
        return session;
    }

    private Message<byte[]> connect(String authorizationHeader, String session) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId(session);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> frame(
            StompCommand command,
            String destination,
            String payload,
            String session) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        accessor.setSessionId(session);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(payload.getBytes(StandardCharsets.UTF_8), accessor.getMessageHeaders());
    }

    private User user(String accountStatus) {
        return User.builder().email(EMAIL).accountStatus(accountStatus).build();
    }
}
