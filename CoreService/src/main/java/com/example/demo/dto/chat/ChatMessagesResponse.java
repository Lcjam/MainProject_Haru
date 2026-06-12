package com.example.demo.dto.chat;

import com.example.demo.model.chat.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagesResponse {
    private boolean success;
    private String message;
    private List<ChatMessage> messages;
    private Integer totalCount;
    private Integer totalPages;
    private Integer currentPage;
}
