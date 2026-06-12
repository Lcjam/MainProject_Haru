package com.example.demo.dao;

import com.example.demo.model.ChatMessage;
import java.util.List;

public interface ChatMessageDAO {
    void saveMessage(ChatMessage message);
    List<ChatMessage> getMessagesBySessionId(String sessionId);
} 