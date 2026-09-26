package com.example.demo.controller;

import com.example.demo.dto.chat.ChatRoomRequest;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.Market.ProductService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/chat/rooms")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final ProductService productService;
    private final TokenUtils tokenUtils;
    private final NotificationService notificationService;

    /**
     * 채팅방 생성/조회
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createChatRoom(
            @RequestHeader("Authorization") String token,
            @RequestBody ChatRoomRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        ChatRoomResponse response = chatService.createOrGetChatRoom(email, request);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage(), "400"));
        }
        
        // 알림 추가
        String message = String.format("\"%s\" 상품에 대한 채팅방이 생성되었습니다!", request.getProductId());
        notificationService.sendNotification(email, message, "CHAT_MESSAGE", response.getChatroomId(), request.getProductId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자의 채팅방 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getChatRoomsByUser(
            @RequestHeader("Authorization") String token) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        ChatRoomResponse response = chatService.getChatRoomsByUser(email);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 모집 중이거나 승인된 채팅방 목록 조회
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<?>> getActiveChatRoomsByUser(
            @RequestHeader("Authorization") String token) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        ChatRoomResponse response = chatService.getActiveChatRoomsByUser(email);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 특정 채팅방 상세 정보 조회
     */
    @GetMapping("/{chatroomId}")
    public ResponseEntity<ApiResponse<?>> getChatRoomDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer chatroomId) {
        
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        ChatRoomResponse response = chatService.getChatRoomDetail(email, chatroomId);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage(), "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 등록자의 함께하기 버튼 처리 (구매 요청 승인)
     */
    @PostMapping("/{chatroomId}/approve")
    public ResponseEntity<?> approveChatMember(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Integer chatroomId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        return productService.approveProductRequestByChatroom(email, chatroomId);
    }

    /**
     * 상품 ID를 통해 채팅방 ID 조회
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> getChatRoomIdByProductId(
            @RequestHeader("Authorization") String token,
            @PathVariable Long productId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // 상품 ID와 사용자 이메일로 채팅방 조회
            ChatRoom chatRoom = chatService.findChatRoomByProductIdAndEmail(productId, email);
            
            if (chatRoom == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("채팅방을 찾을 수 없습니다.", "404"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(chatRoom.getChatroomId()));
        } catch (Exception e) {
            log.error("채팅방 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("채팅방 조회 중 오류가 발생했습니다.", "500"));
        }
    }
}
