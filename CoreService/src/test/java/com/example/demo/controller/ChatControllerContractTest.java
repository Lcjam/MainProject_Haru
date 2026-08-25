package com.example.demo.controller;

import com.example.demo.dto.chat.ChatRoomRequest;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatController 의 현행 HTTP 계약을 고정한다 (Phase 1a — Mapper 직접 주입 → 서비스 이동 전 안전망).
 * Mapper 상호작용은 검증하지 않는다 — 이동 후에도 그대로 그린이어야 하므로 상태코드/ApiResponse 형태만 단언한다.
 * {@code @MessageMapping} 핸들러는 이 클래스에 없다(ChatController 는 순수 HTTP 컨트롤러).
 */
@DisplayName("ChatController contract")
class ChatControllerContractTest {

    private final ChatService chatService = mock(ChatService.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private final ChatRoomMapper chatRoomMapper = mock(ChatRoomMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductRequestMapper productRequestMapper = mock(ProductRequestMapper.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatController(chatService, tokenUtils, chatRoomMapper,
                        productMapper, productRequestMapper, notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- POST /api/core/chat/rooms (createChatRoom) ----

    @Test
    @DisplayName("createChatRoom: 이메일 null이면 401")
    void createChatRoom_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(post("/api/core/chat/rooms")
                        .header("Authorization", "Bearer bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("401"))
                .andExpect(jsonPath("$.data").value("인증되지 않은 요청입니다."));
    }

    @Test
    @DisplayName("createChatRoom: 서비스 실패(isSuccess=false)면 400 + 서비스 메시지")
    void createChatRoom_serviceFailure_returns400() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(chatService.createOrGetChatRoom(anyString(), any(ChatRoomRequest.class)))
                .willReturn(ChatRoomResponse.builder().success(false).message("상품을 찾을 수 없습니다.").build());

        mockMvc.perform(post("/api/core/chat/rooms")
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("상품을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("createChatRoom: 정상 생성은 200 + success")
    void createChatRoom_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(chatService.createOrGetChatRoom(anyString(), any(ChatRoomRequest.class)))
                .willReturn(ChatRoomResponse.builder().success(true).chatroomId(10).build());

        mockMvc.perform(post("/api/core/chat/rooms")
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ---- GET /api/core/chat/rooms (getChatRoomsByUser) ----

    @Test
    @DisplayName("getChatRoomsByUser: 이메일 null이면 401")
    void getChatRoomsByUser_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/chat/rooms").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getChatRoomsByUser: 정상 조회는 200 + success")
    void getChatRoomsByUser_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(chatService.getChatRoomsByUser("user@haru.com"))
                .willReturn(ChatRoomResponse.builder().success(true).build());

        mockMvc.perform(get("/api/core/chat/rooms").header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ---- GET /api/core/chat/rooms/active (getActiveChatRoomsByUser) ----

    @Test
    @DisplayName("getActiveChatRoomsByUser: 이메일 null이면 401")
    void getActiveChatRoomsByUser_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/chat/rooms/active").header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getActiveChatRoomsByUser: 정상 조회는 200 + success")
    void getActiveChatRoomsByUser_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(chatService.getActiveChatRoomsByUser("user@haru.com"))
                .willReturn(ChatRoomResponse.builder().success(true).build());

        mockMvc.perform(get("/api/core/chat/rooms/active").header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ---- GET /api/core/chat/rooms/{chatroomId} (getChatRoomDetail) ----

    @Test
    @DisplayName("getChatRoomDetail: 토큰 무효면 401")
    void getChatRoomDetail_unauthorized() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("bad");
        given(tokenUtils.isTokenValid("bad")).willReturn(false);

        mockMvc.perform(get("/api/core/chat/rooms/{chatroomId}", 1).header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getChatRoomDetail: 서비스 실패(isSuccess=false)면 400 + 서비스 메시지")
    void getChatRoomDetail_serviceFailure_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatService.getChatRoomDetail("user@haru.com", 1))
                .willReturn(ChatRoomResponse.builder().success(false).message("채팅방이 없습니다.").build());

        mockMvc.perform(get("/api/core/chat/rooms/{chatroomId}", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("채팅방이 없습니다."));
    }

    @Test
    @DisplayName("getChatRoomDetail: 정상 조회는 200 + success")
    void getChatRoomDetail_success() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatService.getChatRoomDetail("user@haru.com", 1))
                .willReturn(ChatRoomResponse.builder().success(true).chatroomId(1).build());

        mockMvc.perform(get("/api/core/chat/rooms/{chatroomId}", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ---- POST /api/core/chat/rooms/{chatroomId}/approve (approveChatMember) ----

    @Test
    @DisplayName("approveChatMember: 이메일 null이면 401")
    void approveChatMember_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(post("/api/core/chat/rooms/{chatroomId}/approve", 1).header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("approveChatMember: 채팅방이 없으면 404")
    void approveChatMember_chatRoomNotFound_returns404() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("seller@haru.com");
        given(chatRoomMapper.findChatRoomById(1, "seller@haru.com")).willReturn(null);

        mockMvc.perform(post("/api/core/chat/rooms/{chatroomId}/approve", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.data").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("approveChatMember: 요청자가 등록자(판매자)가 아니면 403")
    void approveChatMember_notSeller_returns403() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("buyer@haru.com");
        ChatRoom chatRoom = ChatRoom.builder().chatroomId(1).sellerEmail("seller@haru.com")
                .productId(5L).requestEmail("buyer@haru.com").build();
        given(chatRoomMapper.findChatRoomById(1, "buyer@haru.com")).willReturn(chatRoom);

        mockMvc.perform(post("/api/core/chat/rooms/{chatroomId}/approve", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403"))
                .andExpect(jsonPath("$.data").value("상품 등록자만 승인할 수 있습니다."));
    }

    @Test
    @DisplayName("approveChatMember: 함께하기 요청을 찾지 못하면 404")
    void approveChatMember_requestNotFound_returns404() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("seller@haru.com");
        ChatRoom chatRoom = ChatRoom.builder().chatroomId(1).sellerEmail("seller@haru.com")
                .productId(5L).requestEmail("buyer@haru.com").build();
        given(chatRoomMapper.findChatRoomById(1, "seller@haru.com")).willReturn(chatRoom);
        given(productRequestMapper.findRequestId(5L, "buyer@haru.com")).willReturn(null);

        mockMvc.perform(post("/api/core/chat/rooms/{chatroomId}/approve", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.data").value("해당 요청을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("approveChatMember: 정상 승인은 200 + success")
    void approveChatMember_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("seller@haru.com");
        ChatRoom chatRoom = ChatRoom.builder().chatroomId(1).sellerEmail("seller@haru.com")
                .productId(5L).requestEmail("buyer@haru.com").build();
        given(chatRoomMapper.findChatRoomById(1, "seller@haru.com")).willReturn(chatRoom);
        given(productRequestMapper.findRequestId(5L, "buyer@haru.com")).willReturn(99L);

        mockMvc.perform(post("/api/core/chat/rooms/{chatroomId}/approve", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value("요청이 승인되었습니다."));
    }

    @Test
    @DisplayName("approveChatMember: 예외 발생 시 500 + 고정 메시지")
    void approveChatMember_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("seller@haru.com");
        given(chatRoomMapper.findChatRoomById(1, "seller@haru.com"))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(post("/api/core/chat/rooms/{chatroomId}/approve", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("요청 처리 중 오류가 발생했습니다."));
    }

    // ---- GET /api/core/chat/rooms/product/{productId} (getChatRoomIdByProductId) ----

    @Test
    @DisplayName("getChatRoomIdByProductId: 이메일 null이면 401")
    void getChatRoomIdByProductId_unauthorized() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn(null);

        mockMvc.perform(get("/api/core/chat/rooms/product/{productId}", 1).header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getChatRoomIdByProductId: 채팅방을 찾지 못하면 404")
    void getChatRoomIdByProductId_notFound_returns404() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(chatRoomMapper.findChatRoomByProductIdAndEmail(1L, "user@haru.com")).willReturn(null);

        mockMvc.perform(get("/api/core/chat/rooms/product/{productId}", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404"))
                .andExpect(jsonPath("$.data").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("getChatRoomIdByProductId: 정상 조회는 200 + chatroomId")
    void getChatRoomIdByProductId_success() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        ChatRoom chatRoom = ChatRoom.builder().chatroomId(7).build();
        given(chatRoomMapper.findChatRoomByProductIdAndEmail(1L, "user@haru.com")).willReturn(chatRoom);

        mockMvc.perform(get("/api/core/chat/rooms/product/{productId}", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value(7));
    }

    @Test
    @DisplayName("getChatRoomIdByProductId: 예외 발생 시 500 + 고정 메시지")
    void getChatRoomIdByProductId_exception_returns500() throws Exception {
        given(tokenUtils.getEmailFromAuthHeader(anyString())).willReturn("user@haru.com");
        given(chatRoomMapper.findChatRoomByProductIdAndEmail(anyLong(), anyString()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(get("/api/core/chat/rooms/product/{productId}", 1).header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("채팅방 조회 중 오류가 발생했습니다."));
    }
}
