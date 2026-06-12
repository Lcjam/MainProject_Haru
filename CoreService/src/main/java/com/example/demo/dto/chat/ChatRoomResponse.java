package com.example.demo.dto.chat;

import com.example.demo.model.chat.ChatRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    private boolean success;
    private String message;
    
    // 단일 채팅방 정보
    private Integer chatroomId;
    private String chatname;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private String registrantEmail;
    private String sellerEmail;
    private String requestEmail;
    private String otherUserEmail; // 상대방 이메일
    private String otherUserName;  // 상대방 닉네임
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private LocalDateTime createdAt;
    
    // 채팅방 목록
    private List<ChatRoom> chatRooms;
}
