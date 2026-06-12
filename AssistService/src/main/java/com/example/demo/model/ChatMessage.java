package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private Long id;
    private String messageType;            // 'user' 또는 'assistant'
    private String content;
    private String sessionId;
    private LocalDateTime createdAt;
} 