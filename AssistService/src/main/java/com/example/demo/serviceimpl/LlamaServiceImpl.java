package com.example.demo.serviceimpl;
import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

import com.example.demo.dao.ChatMessageDAO;
import com.example.demo.model.ChatMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlamaServiceImpl {
    private final CloudChatBotServiceImpl cloudChatBotServiceImpl;
    private final ChatMessageDAO chatMessageDAO;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    
    
    // 게이트웨이(=FastAPI 챗봇 진입점) 주소. 환경설정에서 주입, 기본은 로컬.
    // (이전엔 activeProfile 이 "prod" 로 하드코딩돼 로컬에서도 gateway-container 를 호출하는 버그가 있었음)
    @Value("${gateway.base-url:http://localhost:8080}")
    private String gatewayUri;

    private String translate(String text, String sourceLang, String targetLang) {
        log.debug("gatewayUri: " + gatewayUri);
        log.debug("=== 번역 시작 ===");
        log.debug(String.format("%s -> %s 번역", sourceLang, targetLang));
        log.debug("번역할 텍스트: " + text);
        
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
            log.debug("=== 번역 에러 발생 ===");
            log.debug("번역 에러: " + e.getMessage());
            return text;
        }
    }

    // 번역 API(MyMemory) 자격증명 — 환경변수로 주입(소스 하드코딩 금지). 비어 있어도 저용량은 동작.
    @Value("${translate.api.key:}")
    private String translateApiKey;
    @Value("${translate.api.email:}")
    private String translateApiEmail;

    private String translateChunk(String text, String sourceLang, String targetLang) throws Exception {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl("https://api.mymemory.translated.net/get")
                .queryParam("q", "{text}")
                .queryParam("langpair", "{languages}");
        Map<String, String> parameters = new HashMap<>();
        parameters.put("text", text);
        parameters.put("languages", sourceLang + "|" + targetLang);
        if (translateApiKey != null && !translateApiKey.isBlank()) {
            uri.queryParam("key", "{key}");
            parameters.put("key", translateApiKey);
        }
        if (translateApiEmail != null && !translateApiEmail.isBlank()) {
            uri.queryParam("de", "{email}");
            parameters.put("email", translateApiEmail);
        }
        URI url = uri.encode().buildAndExpand(parameters).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
        String body = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        JsonNode jsonResponse = objectMapper.readTree(body);
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
        log.debug("=== LlamaService 채팅 요청 시작 ===");
        log.debug("받은 메시지: " + message);
        log.debug("히스토리 크기: " + history.size());
        
        String response = null;
        
        try {
            // CloudChatBot 먼저 시도            
            try {
                response = cloudChatBotServiceImpl.getResponse(message).trim();
                log.debug("CloudChatBot 응답: " + response);
                if (response != null && !response.trim().isEmpty() && !response.equals("false")) {
                    // CloudChatBot 응답이 성공적으로 왔을 때 DB에 저장
                    saveChat(message, response, sessionId);
                    return response;
                }
            } catch (Exception e) {
                log.error("CloudChatBot 서비스 호출 실패, Llama 서비스로 전환: " + e.getMessage());
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
            log.error("Chat 처리 중 에러 발생", e);
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
            log.error("채팅 저장 중 오류 발생", e);
        }
    }

    private String processWithLlama(String englishMessage, String sessionId, List<Map<String, String>> history) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "message", englishMessage, "sessionId", sessionId, "history", history);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = restTemplate.postForObject(gatewayUri + "/api/fastapi/chat",
                    new HttpEntity<>(requestBody, headers), String.class);
            return objectMapper.readTree(body).get("response").asText();
        } catch (HttpStatusCodeException e) {
            log.warn("Python 서버 에러 응답: {}", e.getStatusCode());
            return "An error occurred while processing your request.";
        } catch (Exception e) {
            log.error("Llama 서비스 호출 실패", e);
            return "죄송합니다. 서비스 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
