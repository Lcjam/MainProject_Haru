package com.example.demo.service;

import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.model.chat.ChatRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * 단위 1a-3 — ChatMessageController.processMessage({@code @MessageMapping}) 가 직접 쓰던
 * UserMapper.findByEmail / ChatRoomMapper.findChatRoomById 를 ChatMessageService 로 옮긴 뒤의
 * 통과 메서드 단위 테스트. MockMvc 가 {@code @MessageMapping} 핸들러를 태우지 못해 컨트롤러 계약
 * 테스트에서 검증할 수 없으므로 여기서 직접 검증한다.
 */
@DisplayName("ChatMessageService 신규 통과 메서드")
class ChatMessageServiceTest {

    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductRequestMapper productRequestMapper = mock(ProductRequestMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    private final ChannelTopic chatChannelTopic = mock(ChannelTopic.class);

    private final ChatMessageService chatMessageService = new ChatMessageService(
            chatMessageMapper, chatRoomMapper, productMapper, productRequestMapper,
            userMapper, redisTemplate, chatChannelTopic);

    @Test
    @DisplayName("findUserByEmail: UserMapper.findByEmail 를 그대로 위임한다")
    void findUserByEmail_delegatesToMapper() {
        User user = User.builder().email("sender@haru.com").nickname("sender-nick").build();
        given(userMapper.findByEmail("sender@haru.com")).willReturn(user);

        User result = chatMessageService.findUserByEmail("sender@haru.com");

        assertEquals("sender-nick", result.getNickname());
    }

    @Test
    @DisplayName("findUserByEmail: 사용자가 없으면 null 을 그대로 반환한다")
    void findUserByEmail_returnsNullWhenNotFound() {
        given(userMapper.findByEmail("missing@haru.com")).willReturn(null);

        assertNull(chatMessageService.findUserByEmail("missing@haru.com"));
    }

    @Test
    @DisplayName("findChatRoomById: ChatRoomMapper.findChatRoomById 를 그대로 위임한다")
    void findChatRoomById_delegatesToMapper() {
        ChatRoom chatRoom = ChatRoom.builder().chatroomId(1).sellerEmail("seller@haru.com")
                .requestEmail("buyer@haru.com").build();
        given(chatRoomMapper.findChatRoomById(1, "buyer@haru.com")).willReturn(chatRoom);

        ChatRoom result = chatMessageService.findChatRoomById(1, "buyer@haru.com");

        assertEquals("seller@haru.com", result.getSellerEmail());
    }

    @Test
    @DisplayName("findChatRoomById: 채팅방이 없으면 null 을 그대로 반환한다")
    void findChatRoomById_returnsNullWhenNotFound() {
        given(chatRoomMapper.findChatRoomById(99, "buyer@haru.com")).willReturn(null);

        assertNull(chatMessageService.findChatRoomById(99, "buyer@haru.com"));
    }
}
