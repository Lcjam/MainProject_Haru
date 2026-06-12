package com.example.demo.controller;

import com.example.demo.dto.chat.ChatMessageRequest;
import com.example.demo.dto.chat.ChatMessagesResponse;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.model.chat.ChatMessage;
import com.example.demo.model.chat.ChatRoom;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.service.ChatMessageService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.UserService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/core/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final TokenUtils tokenUtils;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final ChatRoomMapper chatRoomMapper;
    private final UserMapper userMapper;

    /**
     * WebSocket을 통한 메시지 전송
     * 인증된 사용자의 WebSocket 세션을 통해 메시지를 처리합니다.
     */
    @MessageMapping("/chat/send")
    public void processMessage(
            @Payload ChatMessageRequest messageRequest,
            SimpMessageHeaderAccessor headerAccessor,
            Principal principal) {
        
        if (principal == null) {
            log.error("WebSocket 메시지 처리 오류: 인증되지 않은 사용자");
            return;
        }
        
        String senderEmail = principal.getName();
        log.info("WebSocket 메시지 수신: chatroomId={}, senderEmail={}", 
                messageRequest.getChatroomId(), senderEmail);
        
        try {
            // 메시지 저장 및 발행
            ChatMessage chatMessage = chatMessageService.sendMessage(senderEmail, messageRequest);
            
            // 메시지를 채팅방의 모든 구독자에게 브로드캐스트
            messagingTemplate.convertAndSend(
                "/topic/room." + messageRequest.getChatroomId(), 
                chatMessage
            );

            // UserMapper를 직접 사용하여 사용자 정보 조회
            User sender = userMapper.findByEmail(senderEmail);
            String senderNickname = sender != null ? sender.getNickname() : "알 수 없음";

            // 알림 메시지 구성
            String notificationChatMessage = String.format("[%s] %s: %s",
                messageRequest.getProductId(),
                senderNickname, 
                messageRequest.getContent().length() > 30 
                    ? messageRequest.getContent().substring(0, 27) + "..." 
                    : messageRequest.getContent()
            );

            // ChatRoomMapper를 직접 사용하여 채팅방 정보 조회
            ChatRoom chatRoom = chatRoomMapper.findChatRoomById(messageRequest.getChatroomId(), senderEmail);
            if (chatRoom == null) {
                log.error("채팅방을 찾을 수 없음: chatroomId={}", messageRequest.getChatroomId());
                return;
            }

            // 수신자 이메일 결정 (발신자가 구매자면 판매자에게, 발신자가 판매자면 구매자에게)
            String receiverEmail;
            if (senderEmail.equals(chatRoom.getRequestEmail())) {
                receiverEmail = chatRoom.getSellerEmail();
            } else {
                receiverEmail = chatRoom.getRequestEmail();
            }

            // 수신자에게 알림 전송
            notificationService.sendNotification(
                receiverEmail,
                notificationChatMessage,
                "CHAT_MESSAGE",
                messageRequest.getChatroomId(),
                messageRequest.getProductId()
            );
            
            log.info("메시지 발행 완료: messageId={}", chatMessage.getMessageId());
        } catch (Exception e) {
            log.error("메시지 처리 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * REST API를 통한 메시지 전송
     * JWT 토큰으로 인증된 사용자의 HTTP 요청을 처리합니다.
     */
    @PostMapping("/messages")
    public ResponseEntity<ApiResponse<?>> sendMessage(
            @RequestHeader("Authorization") String token,
            @RequestBody ChatMessageRequest request) {
        
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        
        try {
            ChatMessage message = chatMessageService.sendMessage(email, request);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (IllegalArgumentException e) {
            log.warn("메시지 전송 검증 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("메시지 전송 중 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 채팅방 메시지 목록 조회
     */
    @GetMapping("/rooms/{chatroomId}/messages")
    public ResponseEntity<ApiResponse<?>> getChatMessages(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer chatroomId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        ChatMessagesResponse response = chatMessageService.getChatMessages(chatroomId, email, page, size);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage(), "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 메시지 읽음 상태 업데이트
     */
    @PutMapping("/rooms/{chatroomId}/messages/read")
    public ResponseEntity<ApiResponse<?>> markMessagesAsRead(
            @RequestHeader("Authorization") String token,
            @PathVariable Integer chatroomId) {
        
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        
        try {
            boolean success = chatMessageService.markMessagesAsRead(chatroomId, email);
            
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("메시지가 읽음 상태로 업데이트 되었습니다."));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("메시지 상태 업데이트 실패", "400"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("메시지 읽음 상태 업데이트 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 이미지 메시지 전송 (REST)
     */
    @PostMapping(value = "/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> sendImageMessage(
            @RequestHeader("Authorization") String token,
            @RequestParam("chatroomId") Integer chatroomId,
            @RequestParam("image") MultipartFile image) {
        
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        if (image == null || image.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("이미지 파일이 필요합니다.", "400"));
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        
        try {
            ChatMessage message = chatMessageService.sendImageMessage(email, chatroomId, image);
            return ResponseEntity.ok(ApiResponse.success(message));
        } catch (Exception e) {
            log.error("이미지 메시지 전송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("이미지 메시지 전송 실패: " + e.getMessage(), "400"));
        }
    }
}
