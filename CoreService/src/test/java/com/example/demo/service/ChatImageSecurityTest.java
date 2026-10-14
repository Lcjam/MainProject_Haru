package com.example.demo.service;

import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Market.Product;
import com.example.demo.model.chat.ChatRoom;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatImageSecurityTest {
    private final ChatMessageMapper messages = mock(ChatMessageMapper.class);
    private final ChatRoomMapper rooms = mock(ChatRoomMapper.class);
    private final ProductMapper products = mock(ProductMapper.class);
    private final FileStorageService files = mock(FileStorageService.class);
    @SuppressWarnings("unchecked")
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final ChatMessageService service = new ChatMessageService(messages, rooms, products,
            mock(ProductRequestMapper.class), mock(UserMapper.class), redis, files, mock(ChannelTopic.class));

    @Test
    void outsiderCannotResolveChatImage() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getChatImagePath(20, "other@haru.test", "file.png"));
        verifyNoInteractions(files);
    }

    @Test
    void databaseFailureRemovesNewChatImage() {
        ChatRoom room = ChatRoom.builder().chatroomId(20).productId(10L)
                .sellerEmail("owner@haru.test").requestEmail("member@haru.test").build();
        when(rooms.findChatRoomById(20, "member@haru.test")).thenReturn(room);
        when(products.findById(10L, "owner@haru.test"))
                .thenReturn(Product.builder().id(10L).isVisible(true).build());
        var image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1});
        when(files.storeFile(image, "chat-images/chatroom_20", "a.png"))
                .thenReturn("/chat-images/chatroom_20/file.png");
        doThrow(new IllegalStateException("db failed")).when(messages).saveChatMessage(any());

        assertThrows(RuntimeException.class,
                () -> service.sendImageMessage("member@haru.test", 20, image));
        verify(files).deleteStoredFile("/chat-images/chatroom_20/file.png");
    }
}
