package com.example.demo.controller;

import com.example.demo.dto.chat.ChatMessagesResponse;
import com.example.demo.exception.GlobalExceptionHandler;
import com.example.demo.model.chat.ChatMessage;
import com.example.demo.service.ChatMessageService;
import com.example.demo.service.NotificationService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatMessageController 의 현행 HTTP 계약을 고정한다 (Phase 1a).
 * {@code @MessageMapping("/chat/send")} (processMessage) 는 MockMvc 로 태울 수 없으므로 이 클래스에서 제외한다
 * — 별도 단위에서 ChatMessageService 의 Mockito 단위 테스트로 다룬다.
 * Mapper 상호작용은 검증하지 않는다 — HTTP 계약(상태코드/ApiResponse 형태)만 단언한다.
 */
@DisplayName("ChatMessageController contract (HTTP 엔드포인트만)")
class ChatMessageControllerContractTest {

    private final ChatMessageService chatMessageService = mock(ChatMessageService.class);
    private final TokenUtils tokenUtils = mock(TokenUtils.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatMessageController(chatMessageService, tokenUtils, messagingTemplate,
                        notificationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ---- POST /api/core/chat/messages (sendMessage) ----

    @Test
    @DisplayName("sendMessage: 토큰 무효면 401")
    void sendMessage_unauthorized() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("bad");
        given(tokenUtils.isTokenValid("bad")).willReturn(false);

        mockMvc.perform(post("/api/core/chat/messages")
                        .header("Authorization", "Bearer bad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatroomId\":1,\"content\":\"hi\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("sendMessage: IllegalArgumentException이면 400 + 메시지")
    void sendMessage_illegalArgument_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.sendMessage(anyString(), any()))
                .willThrow(new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        mockMvc.perform(post("/api/core/chat/messages")
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatroomId\":1,\"content\":\"hi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("sendMessage: 정상 전송은 200 + success")
    void sendMessage_success() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.sendMessage(anyString(), any()))
                .willReturn(ChatMessage.builder().messageId(1).chatroomId(1).content("hi").build());

        mockMvc.perform(post("/api/core/chat/messages")
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatroomId\":1,\"content\":\"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("sendMessage: 일반 예외면 500 + 고정 메시지")
    void sendMessage_genericException_returns500() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.sendMessage(anyString(), any()))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(post("/api/core/chat/messages")
                        .header("Authorization", "Bearer ok")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chatroomId\":1,\"content\":\"hi\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("메시지 전송 중 오류가 발생했습니다."));
    }

    // ---- GET /api/core/chat/rooms/{chatroomId}/messages (getChatMessages) ----

    @Test
    @DisplayName("getChatMessages: 토큰 무효면 401")
    void getChatMessages_unauthorized() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("bad");
        given(tokenUtils.isTokenValid("bad")).willReturn(false);

        mockMvc.perform(get("/api/core/chat/rooms/{chatroomId}/messages", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("getChatMessages: 서비스 실패(isSuccess=false)면 400 + 서비스 메시지")
    void getChatMessages_serviceFailure_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.getChatMessages(1, "user@haru.com", null, null))
                .willReturn(ChatMessagesResponse.builder().success(false).message("채팅방을 찾을 수 없습니다.").build());

        mockMvc.perform(get("/api/core/chat/rooms/{chatroomId}/messages", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("채팅방을 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("getChatMessages: 정상 조회는 200 + success")
    void getChatMessages_success() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.getChatMessages(1, "user@haru.com", null, null))
                .willReturn(ChatMessagesResponse.builder().success(true).build());

        mockMvc.perform(get("/api/core/chat/rooms/{chatroomId}/messages", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    // ---- PUT /api/core/chat/rooms/{chatroomId}/messages/read (markMessagesAsRead) ----

    @Test
    @DisplayName("markMessagesAsRead: 토큰 무효면 401")
    void markMessagesAsRead_unauthorized() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("bad");
        given(tokenUtils.isTokenValid("bad")).willReturn(false);

        mockMvc.perform(put("/api/core/chat/rooms/{chatroomId}/messages/read", 1)
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("markMessagesAsRead: 서비스가 false를 반환하면 400 + 고정 메시지")
    void markMessagesAsRead_serviceFalse_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.markMessagesAsRead(1, "user@haru.com")).willReturn(false);

        mockMvc.perform(put("/api/core/chat/rooms/{chatroomId}/messages/read", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("메시지 상태 업데이트 실패"));
    }

    @Test
    @DisplayName("markMessagesAsRead: 정상 처리는 200 + success")
    void markMessagesAsRead_success() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.markMessagesAsRead(1, "user@haru.com")).willReturn(true);

        mockMvc.perform(put("/api/core/chat/rooms/{chatroomId}/messages/read", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data").value("메시지가 읽음 상태로 업데이트 되었습니다."));
    }

    @Test
    @DisplayName("markMessagesAsRead: IllegalArgumentException이면 400 + 메시지")
    void markMessagesAsRead_illegalArgument_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.markMessagesAsRead(1, "user@haru.com"))
                .willThrow(new IllegalArgumentException("잘못된 채팅방입니다."));

        mockMvc.perform(put("/api/core/chat/rooms/{chatroomId}/messages/read", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("잘못된 채팅방입니다."));
    }

    @Test
    @DisplayName("markMessagesAsRead: 일반 예외면 500 + 고정 메시지")
    void markMessagesAsRead_genericException_returns500() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        given(chatMessageService.markMessagesAsRead(1, "user@haru.com"))
                .willThrow(new RuntimeException("DB 오류"));

        mockMvc.perform(put("/api/core/chat/rooms/{chatroomId}/messages/read", 1)
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("500"))
                .andExpect(jsonPath("$.data").value("서버 오류가 발생했습니다."));
    }

    // ---- POST /api/core/chat/messages/image (sendImageMessage) ----

    @Test
    @DisplayName("sendImageMessage: 토큰 무효면 401")
    void sendImageMessage_unauthorized() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("bad");
        given(tokenUtils.isTokenValid("bad")).willReturn(false);
        MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/api/core/chat/messages/image")
                        .file(image)
                        .param("chatroomId", "1")
                        .header("Authorization", "Bearer bad"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("401"));
    }

    @Test
    @DisplayName("sendImageMessage: 이미지가 비어있으면 400")
    void sendImageMessage_emptyImage_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        MockMultipartFile emptyImage = new MockMultipartFile("image", "a.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/core/chat/messages/image")
                        .file(emptyImage)
                        .param("chatroomId", "1")
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("이미지 파일이 필요합니다."));
    }

    @Test
    @DisplayName("sendImageMessage: 정상 전송은 200 + success")
    void sendImageMessage_success() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});
        given(chatMessageService.sendImageMessage(anyString(), anyInt(), any()))
                .willReturn(ChatMessage.builder().messageId(1).chatroomId(1).messageType("IMAGE").build());

        mockMvc.perform(multipart("/api/core/chat/messages/image")
                        .file(image)
                        .param("chatroomId", "1")
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.code").value("200"));
    }

    @Test
    @DisplayName("sendImageMessage: 예외 발생 시 400(현행 동작 — 500이 아니라 400으로 매핑됨)")
    void sendImageMessage_exception_returns400() throws Exception {
        given(tokenUtils.extractTokenWithoutBearer(anyString())).willReturn("ok");
        given(tokenUtils.isTokenValid("ok")).willReturn(true);
        given(tokenUtils.getEmailFromToken("ok")).willReturn("user@haru.com");
        MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1, 2, 3});
        given(chatMessageService.sendImageMessage(anyString(), anyInt(), any()))
                .willThrow(new RuntimeException("업로드 실패"));

        mockMvc.perform(multipart("/api/core/chat/messages/image")
                        .file(image)
                        .param("chatroomId", "1")
                        .header("Authorization", "Bearer ok"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.data").value("이미지 메시지 전송 실패: 업로드 실패"));
    }
}
