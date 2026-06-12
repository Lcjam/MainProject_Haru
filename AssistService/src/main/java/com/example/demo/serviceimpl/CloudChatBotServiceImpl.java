package com.example.demo.serviceimpl;

import com.example.demo.service.CloudChatBotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudChatBotServiceImpl implements CloudChatBotService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.cloud.chatbot.secret.key}")
    private String secretKey;

    @Value("${naver.cloud.chatbot.api.url}")
    private String apiUrl;

    @Override
    public String getResponse(String message) {
        try {
            String requestBody = getReqMessage(message);
            
            String signature = makeSignature(requestBody);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-NCP-CHATBOT_SIGNATURE", signature);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return extractResponseMessage(objectMapper.readTree(response.getBody()));
            }
            throw new RuntimeException("챗봇 서버 응답 오류: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("챗봇 요청 처리 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("챗봇 서비스 오류: " + e.getMessage());
        }
    }

    private String makeSignature(String message) {
        try {
            byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String signatureHeader = Base64.getEncoder().encodeToString(signature);
            return signatureHeader;
        } catch (Exception e) {
            throw new RuntimeException("시그니처 생성 실패", e);
        }
    }

    private String getReqMessage(String message) {
        try {
            JSONObject obj = new JSONObject();
            long timestamp = System.currentTimeMillis();

            obj.put("version", "v2");
            obj.put("userId", "user-" + UUID.randomUUID().toString());
            obj.put("timestamp", timestamp);

            JSONObject bubbleObj = new JSONObject();
            bubbleObj.put("type", "text");

            JSONObject dataObj = new JSONObject();
            dataObj.put("description", message);

            bubbleObj.put("data", dataObj);

            JSONArray bubblesArray = new JSONArray();
            bubblesArray.put(bubbleObj);

            obj.put("bubbles", bubblesArray);
            obj.put("event", "send");

            return obj.toString();
        } catch (Exception e) {
            throw new RuntimeException("요청 메시지 생성 실패", e);
        }
    }

    private String extractResponseMessage(JsonNode jsonResponse) {
        JsonNode bubbles = jsonResponse.path("bubbles");
        if (!bubbles.isEmpty()) {
            JsonNode description = bubbles.path(0).path("data").path("description");
            if (!description.isMissingNode()) {
                return description.asText();
            }
        }
        return "응답을 처리할 수 없습니다.";
    }
}