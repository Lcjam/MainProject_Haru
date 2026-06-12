package com.example.demo.model.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    public enum MessageType {
        TEXT, IMAGE, FILE, ENTER, LEAVE, OFFER
    }
    
    private Integer messageId;
    private Integer chatroomId;
    private String senderEmail;
    private String content;
    private String messageType;
    private LocalDateTime sentAt;
    private boolean isRead;
    
    // 추가 필드 - 실시간 전송 및 UI 표시용
    private String senderName;
    private String senderProfileUrl;
    private Long productId;
}
