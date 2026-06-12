package com.example.demo.model.chat;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
    private Integer chatroomId;
    private String chatname;
    private Long productId;
    private String requestEmail;
    private String sellerEmail; // JOIN으로 조회됨
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private String status; // ACTIVE, COMPLETED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer unreadCount; // 읽지 않은 메시지 수
    
    // 추가 정보 (조인 결과)
    private String productName;
    private String productImageUrl;
    private String otherUserName;
}