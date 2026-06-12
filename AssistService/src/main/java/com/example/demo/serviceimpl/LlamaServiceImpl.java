package com.example.demo.serviceimpl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.model.ChatMessage;

@Service
@RequiredArgsConstructor
public class LlamaServiceImpl {
    private final CloudChatBotServiceImpl cloudChatBotServiceImpl;
    private final ChatMessageDAO chatMessageDAO;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    
    private String activeProfile = "prod";

    final String gatewayUri = "prod".equals(activeProfile)
            ? "http://gateway-container:8080"
            : "http://localhost:8080";

    private String translate(String text, String sourceLang, String targetLang) {
        System.out.println("gatewayUri: " + gatewayUri);
        System.out.println("=== 번역 시작 ===");
        System.out.println(String.format("%s -> %s 번역", sourceLang, targetLang));
        System.out.println("번역할 텍스트: " + text);
        
        try {
            if (text.length() <= 500) {
                return translateChunk(text, sourceLang, targetLang);
            }

            // 500자 이상인 경우 문장 단위로 분할
            List<String> chunks = new ArrayList<>();
            StringBuilder currentChunk = new StringBuilder();
            
            // 문장 단위로 분할 (마침표, 느낌표, 물음표 기준)
            String[] sentences = text.split("(?<=[.!?])\\s+");
            
            for (String sentence : sentences) {
                if (currentChunk.length() + sentence.length() > 500) { // 여유 있게 500자로 제한
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                }
                currentChunk.append(sentence).append(" ");
            }
            
            if (currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
            }

            // 각 청크 번역 후 결합
            StringBuilder translatedText = new StringBuilder();
            for (String chunk : chunks) {
                Thread.sleep(1000);
                String translatedChunk = translateChunk(chunk, sourceLang, targetLang);
                translatedText.append(translatedChunk).append(" ");
            }

            return translatedText.toString().trim();

        } catch (Exception e) {
            System.out.println("=== 번역 에러 발생 ===");
            System.out.println("번역 에러: " + e.getMessage());
            e.printStackTrace();
            return text;
        }
    }

    private String translateChunk(String text, String sourceLang, String targetLang) throws Exception {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
        String apiKey = "f3bddd536ff4fe3b5e96";
        String email = "rlatjsql11@gmail.com";
        // rlatjsql11@gmail.com f3bddd536ff4fe3b5e96
        // rlatjsql12@gmail.com 94dea587aaa15e058ead
        String urlStr = String.format(
            "https://api.mymemory.translated.net/get?q=%s&langpair=%s|%s&key=%s&de=%s",
            encodedText, sourceLang, targetLang, apiKey, email
        );
        
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
       

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode jsonResponse = objectMapper.readTree(response.toString());
        String translatedText = jsonResponse.get("responseData").get("translatedText").asText();
        
        // JSON 형식이 포함된 경우 제거
        if (translatedText.startsWith("{\"response\":")) {
            translatedText = translatedText.substring(12, translatedText.length() - 2);
        }
        
        // 불필요한 이스케이프 문자와 따옴표 제거
        translatedText = translatedText.replace("\\", "").replace("\"", "");
        
        return translatedText;
    }

    public String chat(String message, String sessionId, List<ChatMessage> history) {
        System.out.println("=== LlamaService 채팅 요청 시작 ===");
        System.out.println("받은 메시지: " + message);
        System.out.println("히스토리 크기: " + history.size());
        
        String response = null;
        
        try {
            // CloudChatBot 먼저 시도            
            try {
                response = cloudChatBotServiceImpl.getResponse(message).trim();
                System.out.println("CloudChatBot 응답: " + response);
                if (response != null && !response.trim().isEmpty() && !response.equals("false")) {
                    // CloudChatBot 응답이 성공적으로 왔을 때 DB에 저장
                    saveChat(message, response, sessionId);
                    return response;
                }
            } catch (Exception e) {
                System.err.println("CloudChatBot 서비스 호출 실패, Llama 서비스로 전환: " + e.getMessage());
                // CloudChatBot 실패 시 계속 진행 (Llama 사용)
            }

            // CloudChatBot이 실패했거나 응답이 비어있는 경우 Llama 모델 사용
            // 한글 -> 영어 번역
            String translatedMessage = translate(message, "ko", "en");

            // 히스토리도 번역하여 파이썬 서버가 기대하는 형식으로 변환
            List<Map<String, String>> translatedHistory = new ArrayList<>();
            for (int i = 0; i < history.size(); i += 2) {
                Map<String, String> conversation = new HashMap<>();
                // 사용자 메시지
                String userMessage = translate(history.get(i).getContent(), "ko", "en");
                conversation.put("user", userMessage);
                
                // 어시스턴트 응답이 있는 경우
                if (i + 1 < history.size()) {
                    String assistantMessage = translate(history.get(i + 1).getContent(), "ko", "en");
                    conversation.put("assistant", assistantMessage);
                }
                translatedHistory.add(conversation);
            }
            
            // LLaMA 서버에 영어로 요청 (히스토리 포함)
            String englishResponse = processWithLlama(translatedMessage, sessionId, translatedHistory);
            
            // 영어 -> 한글 번역
            response = translate(englishResponse, "en", "ko");
            
            // 최종 한글 응답을 DB에 저장
            saveChat(message, response, sessionId);
            
            return response;
        } catch (Exception e) {
            System.err.println("Chat 처리 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            return "죄송합니다. 서비스 오류가 발생했습니다: " + e.getMessage();
        }
    }

    /**
     * 채팅 내용을 저장합니다.
     * @param userMessage 사용자 메시지
     * @param assistantResponse AI 응답
     * @param sessionId 사용자 이메일 (세션 식별자)
     */
    private void saveChat(String userMessage, String assistantResponse, String sessionId) {
        try {
            // 사용자 메시지 저장
            ChatMessage userChatMessage = new ChatMessage();
            userChatMessage.setSessionId(sessionId);
            userChatMessage.setMessageType("user");
            userChatMessage.setContent(userMessage);
            chatMessageDAO.saveMessage(userChatMessage);

            // AI 응답 저장
            ChatMessage assistantChatMessage = new ChatMessage();
            assistantChatMessage.setSessionId(sessionId);
            assistantChatMessage.setMessageType("assistant");
            assistantChatMessage.setContent(assistantResponse);
            chatMessageDAO.saveMessage(assistantChatMessage);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("채팅 저장 중 오류 발생: " + e.getMessage());
        }
    }

    private String processWithLlama(String englishMessage, String sessionId, List<Map<String, String>> history) {
        try {
            URL url = new URL(gatewayUri + "/api/fastapi/chat");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", englishMessage);
            requestBody.put("sessionId", sessionId);
            requestBody.put("history", history);          
           

            String jsonInputString = objectMapper.writeValueAsString(requestBody);
            System.out.println("Python 서버로 보내는 데이터: " + jsonInputString);
            
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(jsonInputString);
            }

            if (conn.getResponseCode() >= 400) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.out.println("Python 서버 에러 응답: " + errorResponse.toString());
                    return "An error occurred while processing your request.";
                }
            }

            // LLaMA 응답 읽기
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            // JSON 응답 파싱
            JsonNode jsonResponse = objectMapper.readTree(response.toString());
            String englishResponse = jsonResponse.get("response").asText();
            System.out.println("영어 응답: " + englishResponse);

            return englishResponse;  // 영어 응답만 반환
            
        } catch (Exception e) {
            System.out.println("=== LlamaService 에러 발생 ===");
            System.out.println("에러 메시지: " + e.getMessage());
            e.printStackTrace();
            return "죄송합니다. 서비스 오류가 발생했습니다: " + e.getMessage();
        }
    }
} 
