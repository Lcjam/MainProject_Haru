package com.example.demo.service;

import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * ChatController 의 상품별 채팅방 조회 위임을 고정한다.
 */
@DisplayName("ChatService 신규 메서드 (Mapper 직접 주입 제거분)")
class ChatServiceTest {

    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final ChatMessageMapper chatMessageMapper = mock(ChatMessageMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final ProductImageMapper productImageMapper = mock(ProductImageMapper.class);
    private final ProductRequestMapper productRequestMapper = mock(ProductRequestMapper.class);

    private final ChatService chatService = new ChatService(
            chatRoomMapper, chatMessageMapper, productMapper, userMapper,
            productImageMapper, productRequestMapper);

    @Test
    @DisplayName("findChatRoomByProductIdAndEmail: ChatRoomMapper.findChatRoomByProductIdAndEmail 를 그대로 위임한다")
    void findChatRoomByProductIdAndEmail_delegatesToMapper() {
        given(chatRoomMapper.findChatRoomByProductIdAndEmail(1L, "user@haru.com")).willReturn(null);

        assertNull(chatService.findChatRoomByProductIdAndEmail(1L, "user@haru.com"));
    }

}
