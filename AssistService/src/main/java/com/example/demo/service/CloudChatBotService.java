package com.example.demo.service;

import org.springframework.stereotype.Service;

@Service
public interface CloudChatBotService {
    String getResponse(String message);
} 