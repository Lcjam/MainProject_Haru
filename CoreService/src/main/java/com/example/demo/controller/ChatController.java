package com.example.demo.controller;

import com.example.demo.dto.chat.ChatRoomRequest;
import com.example.demo.dto.chat.ChatRoomResponse;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.service.ChatService;
import com.example.demo.service.NotificationService;
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
    private final TokenUtils tokenUtils;
    private final ChatRoomMapper chatRoomMapper;
    private final ProductMapper productMapper;
    private final ProductRequestMapper productRequestMapper;
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
    public ResponseEntity<ApiResponse<?>> approveChatMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer chatroomId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // 채팅방 정보 조회
            ChatRoom chatRoom = chatRoomMapper.findChatRoomById(chatroomId, email);
            
            // 채팅방이 없거나 요청자가 등록자가 아닌 경우
            if (chatRoom == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("채팅방을 찾을 수 없습니다.", "404"));
            }
            
            // 상품 등록자만 승인 가능
            if (!chatRoom.getSellerEmail().equals(email)) {
                return ResponseEntity.status(403).body(ApiResponse.error("상품 등록자만 승인할 수 있습니다.", "403"));
            }
            
            // ProductRequests 테이블에서 해당 요청 찾기
            Long productId = chatRoom.getProductId();
            String requesterEmail = chatRoom.getRequestEmail();
            
            // 요청 정보 조회
            Long requestId = productRequestMapper.findRequestId(productId, requesterEmail);
            
            if (requestId == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("해당 요청을 찾을 수 없습니다.", "404"));
            }
            
            // 요청 승인 처리
            productMapper.updateRequestApprovalStatus(requestId, "승인");
            
            // 현재 모집 인원 증가
            productMapper.increaseCurrentParticipants(productId);
            
            // 모집 인원 충족 시 모집 마감 처리
            productMapper.updateProductVisibility(productId);

            // 알림 추가
            String message = String.format("\"%s\" 상품에 대한 함께하기 요청이 승인되었습니다!", productId);
            notificationService.sendNotification(requesterEmail, message, "CHAT_MESSAGE", chatroomId, productId);
            
            return ResponseEntity.ok(ApiResponse.success("요청이 승인되었습니다."));
        } catch (Exception e) {
            log.error("요청 승인 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("요청 처리 중 오류가 발생했습니다.", "500"));
        }
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
            ChatRoom chatRoom = chatRoomMapper.findChatRoomByProductIdAndEmail(productId, email);
            
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
