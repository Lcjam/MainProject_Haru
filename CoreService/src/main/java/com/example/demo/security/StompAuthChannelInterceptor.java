package com.example.demo.security;

import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.util.TokenUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** STOMP CONNECT 인증과 SUBSCRIBE/SEND 목적지 권한을 검사한다. */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);
    private static final List<String> ROOM_SUBSCRIPTION_PREFIXES = List.of(
            "/topic/room.", "/topic/chat.", "/topic/location.");

    private final TokenUtils tokenUtils;
    private final UserMapper userMapper;
    private final ChatRoomMapper chatRoomMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, String> sessionTokens = new ConcurrentHashMap<>();

    public StompAuthChannelInterceptor(
            TokenUtils tokenUtils,
            UserMapper userMapper,
            ChatRoomMapper chatRoomMapper) {
        this.tokenUtils = tokenUtils;
        this.userMapper = userMapper;
        this.chatRoomMapper = chatRoomMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        try {
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                return authenticateConnect(accessor) ? message : reject(accessor);
            }
            if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
                if (accessor.getSessionId() != null) {
                    sessionTokens.remove(accessor.getSessionId());
                }
                return message;
            }
            if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                    && !StompCommand.SEND.equals(accessor.getCommand())) {
                return message;
            }

            String email = authenticateSession(accessor);
            if (email == null) {
                return reject(accessor);
            }

            boolean allowed = StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                    ? canSubscribe(accessor.getDestination(), email)
                    : canSend(accessor.getDestination(), message.getPayload(), email);
            return allowed ? message : reject(accessor);
        } catch (Exception ignored) {
            return reject(accessor);
        }
    }

    public Message<?> preSendOutbound(Message<?> message) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
        if (accessor.getMessageType() != SimpMessageType.MESSAGE) {
            return message;
        }

        try {
            String email = authenticateSession(accessor.getSessionId());
            return email != null && canSubscribe(accessor.getDestination(), email)
                    ? message : rejectOutbound();
        } catch (Exception ignored) {
            return rejectOutbound();
        }
    }

    private boolean authenticateConnect(StompHeaderAccessor accessor) {
        List<String> authorization = accessor.getNativeHeader("Authorization");
        if (authorization == null || authorization.size() != 1) {
            return false;
        }

        String token = tokenUtils.extractTokenWithoutBearer(authorization.get(0));
        String email = activeEmail(token);
        if (email == null) {
            return false;
        }

        String sessionId = accessor.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        sessionTokens.put(sessionId, token);
        accessor.setUser(new StompPrincipal(email));
        return true;
    }

    private String authenticateSession(StompHeaderAccessor accessor) {
        String email = authenticateSession(accessor.getSessionId());
        if (email != null) {
            accessor.setUser(new StompPrincipal(email));
        }
        return email;
    }

    private String authenticateSession(String sessionId) {
        String token = sessionTokens.get(sessionId);
        if (token == null) {
            return null;
        }

        return activeEmail(token);
    }

    private String activeEmail(String token) {
        String email = tokenUtils.getEmailFromToken(token);
        User user = email == null ? null : userMapper.findByEmail(email);
        return user != null && "Active".equals(user.getAccountStatus()) ? email : null;
    }

    private boolean canSubscribe(String destination, String email) {
        if (destination == null) {
            return false;
        }
        if (destination.equals("/topic/user/" + email)) {
            return true;
        }

        Integer chatroomId = roomSubscriptionId(destination);
        return chatroomId != null && chatRoomMapper.findChatRoomById(chatroomId, email) != null;
    }

    private boolean canSend(String destination, Object payload, String email) {
        Integer payloadChatroomId = payloadChatroomId(payload);
        if (payloadChatroomId == null) {
            return false;
        }

        if (!"/app/chat/send".equals(destination)) {
            Integer destinationChatroomId = positiveIdAfter(destination, "/app/location/");
            if (!payloadChatroomId.equals(destinationChatroomId)) {
                return false;
            }
        }
        return ("/app/chat/send".equals(destination) || destination.startsWith("/app/location/"))
                && chatRoomMapper.findChatRoomById(payloadChatroomId, email) != null;
    }

    private Integer roomSubscriptionId(String destination) {
        for (String prefix : ROOM_SUBSCRIPTION_PREFIXES) {
            Integer id = positiveIdAfter(destination, prefix);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private Integer positiveIdAfter(String value, String prefix) {
        if (value == null || !value.startsWith(prefix)) {
            return null;
        }
        try {
            String suffix = value.substring(prefix.length());
            int id = Integer.parseInt(suffix);
            return id > 0 && suffix.equals(Integer.toString(id)) ? id : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer payloadChatroomId(Object payload) {
        try {
            JsonNode body = payload instanceof byte[] bytes
                    ? objectMapper.readTree(bytes)
                    : objectMapper.readTree(String.valueOf(payload));
            JsonNode id = body == null ? null : body.get("chatroomId");
            if (id == null || !id.isIntegralNumber() || !id.canConvertToInt() || id.intValue() <= 0) {
                return null;
            }
            return id.intValue();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Message<?> reject(StompHeaderAccessor accessor) {
        log.warn("STOMP frame rejected: command={}", accessor.getCommand());
        return null;
    }

    private Message<?> rejectOutbound() {
        log.warn("STOMP outbound message rejected");
        return null;
    }

    private record StompPrincipal(String name) implements Principal {
        @Override
        public String getName() {
            return name;
        }
    }
}
