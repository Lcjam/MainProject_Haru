package com.example.demo.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Integer chatroomId;
    private Long productId;
    private String senderEmail;
    private String content;
    private String messageType; // TEXT, IMAGE, FILE, OFFER
}
