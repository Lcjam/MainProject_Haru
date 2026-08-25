package com.example.demo.service;

import com.example.demo.mapper.ChatMessageMapper;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductImageMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.chat.ChatRoom;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 단위 1a-3 — ChatController 가 직접 쓰던 ChatRoomMapper/ProductRequestMapper/ProductMapper 호출을
 * ChatService 로 옮긴 뒤의 신규 메서드 단위 테스트. approveChatMember 의 상태코드 분기(404/403)는
 * 컨트롤러 계약 테스트(ChatControllerContractTest)에서 검증하므로, 여기서는 옮겨진 쓰기 시퀀스와
 * 읽기 통과 메서드 자체의 동작만 검증한다.
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
    @DisplayName("findChatRoomById: ChatRoomMapper.findChatRoomById 를 그대로 위임한다")
    void findChatRoomById_delegatesToMapper() {
        ChatRoom chatRoom = ChatRoom.builder().chatroomId(1).sellerEmail("seller@haru.com").build();
        given(chatRoomMapper.findChatRoomById(1, "seller@haru.com")).willReturn(chatRoom);

        assertEquals("seller@haru.com", chatService.findChatRoomById(1, "seller@haru.com").getSellerEmail());
    }

    @Test
    @DisplayName("findChatRoomByProductIdAndEmail: ChatRoomMapper.findChatRoomByProductIdAndEmail 를 그대로 위임한다")
    void findChatRoomByProductIdAndEmail_delegatesToMapper() {
        given(chatRoomMapper.findChatRoomByProductIdAndEmail(1L, "user@haru.com")).willReturn(null);

        assertNull(chatService.findChatRoomByProductIdAndEmail(1L, "user@haru.com"));
    }

    @Test
    @DisplayName("findRequestId: ProductRequestMapper.findRequestId 를 그대로 위임한다")
    void findRequestId_delegatesToMapper() {
        given(productRequestMapper.findRequestId(5L, "buyer@haru.com")).willReturn(99L);

        assertEquals(99L, chatService.findRequestId(5L, "buyer@haru.com"));
    }

    @Test
    @DisplayName("approveChatRequest: 승인 상태 변경 → 참가자 수 증가 → 노출 갱신, 3단계 쓰기를 그대로 수행한다")
    void approveChatRequest_performsThreeWritesInOrder() {
        chatService.approveChatRequest(99L, 5L);

        verify(productMapper).updateRequestApprovalStatus(99L, "승인");
        verify(productMapper).increaseCurrentParticipants(5L);
        verify(productMapper).updateProductVisibility(5L);
    }

    /**
     * 단위 1a-4 — 중간 실패 시 후속 쓰기가 일어나지 않음을 고정한다.
     * 주의: 이 테스트는 "후속 매퍼 호출이 없다"까지만 보장한다. 앞선 쓰기가 실제로 롤백되는지는
     * DB 를 태워야 알 수 있고, 그 레인은 Phase 2 소관이다. @Transactional 이 붙어 있다는 사실만으로
     * 롤백을 검증했다고 주장하지 말 것.
     */
    @Test
    @DisplayName("approveChatRequest: 참가자 수 증가에서 실패하면 노출 갱신은 호출되지 않는다")
    void approveChatRequest_middleFailure_skipsSubsequentWrites() {
        willThrow(new RuntimeException("DB 오류")).given(productMapper).increaseCurrentParticipants(5L);

        assertThrows(RuntimeException.class, () -> chatService.approveChatRequest(99L, 5L));

        verify(productMapper).updateRequestApprovalStatus(99L, "승인");
        verify(productMapper, never()).updateProductVisibility(5L);
    }
}
