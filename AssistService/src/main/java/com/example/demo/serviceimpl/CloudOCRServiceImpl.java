package com.example.demo.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudOCRServiceImpl {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${naver.cloud.ocr.secret.key}")
    private String secretKey;

    @Value("${naver.cloud.ocr.api.url}")
    private String apiUrl;

    public String processImage(MultipartFile file) {
        log.debug("=== CloudOCRService 이미지 처리 시작 ===");
        try {
            // 1. 파일 형식 검증
            String fileFormat = file.getContentType().split("/")[1].toLowerCase();
            if (!Arrays.asList("jpg", "jpeg", "png", "pdf", "tif", "tiff").contains(fileFormat)) {
                throw new IllegalArgumentException("지원하지 않는 파일 형식: " + fileFormat);
            }
            log.info("=== 파일 정보 ===");
            log.info("파일명: {}", file.getOriginalFilename());
            log.info("파일 크기: {} bytes", file.getSize());
            log.info("파일 형식: {}", fileFormat);

            // 2. Base64 인코딩
            String imageBase64 = Base64.getEncoder().encodeToString(file.getBytes());
            log.info("Base64 인코딩 길이: {}", imageBase64.length());

            // 3. API 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("version", "V2");
            requestBody.put("requestId", UUID.randomUUID().toString());
            requestBody.put("timestamp", System.currentTimeMillis());
            requestBody.put("lang", "ko");

            Map<String, Object> imageInfo = new HashMap<>();
            imageInfo.put("format", fileFormat);
            imageInfo.put("name", "카드");
            imageInfo.put("data", imageBase64);
            imageInfo.put("templateIds", Collections.singletonList(36479));
            

            requestBody.put("images", Collections.singletonList(imageInfo));

            log.info("OCR 요청 시작 - requestId: {}", requestBody.get("requestId"));

            // 4. API 요청 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-OCR-SECRET", secretKey);

            // 5. API 요청 실행
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            // 6. 응답 처리
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("OCR 요청 성공");
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                log.info("=== OCR 응답 정보 ===");
                JsonNode images = responseJson.path("images");
                if (images.isArray() && images.size() > 0) {
                    JsonNode image = images.get(0);
                    log.info("인식 결과: {}", image.path("inferResult").asText());
                    log.info("메시지: {}", image.path("message").asText());
                    log.info("matchedTemplate.id : {}", image.path("matchedTemplate.id").asText());
                    log.info("combineResult: {}", image.path("combineResult").asText());
                    log.info("combineResult.text: {}", image.path("combineResult.text").asText());
                    log.info("combineResult.name: {}", image.path("combineResult.name").asText());
                    log.info("name: {}", image.path("name").asText());

                    JsonNode fields = image.path("fields");
                    if (fields.isArray()) {
                        log.info("추출된 필드 수: {}", fields.size());
                        for (JsonNode field : fields) {
                            log.info("텍스트: {} (신뢰도: {})", 
                                field.path("inferText").asText(),
                                field.path("inferConfidence").floatValue());
                        }
                    }
                }
                
                String extractedText = extractText(responseJson);
                log.info("=== 최종 추출 텍스트 ===\n{}", extractedText);
                return extractedText;
            }

            log.error("OCR 서버 응답 오류: {}", response.getStatusCode());
            throw new RuntimeException("OCR 서버 응답 오류: " + response.getStatusCode());

        } catch (Exception e) {
            log.error("OCR 처리 중 오류 발생", e);
            throw new RuntimeException("OCR 서비스 오류: " + e.getMessage());
        }
    }

    private String extractText(JsonNode jsonResponse) {
        StringBuilder result = new StringBuilder();
        JsonNode images = jsonResponse.path("images");
    
        if (images.isArray()) {
            for (JsonNode image : images) {
                log.info("=== OCR 개별 이미지 응답 ===");
                log.info("이미지 UID: {}", image.path("uid").asText());
                log.info("인식 결과: {}", image.path("inferResult").asText());
                log.info("메시지: {}", image.path("message").asText());
                log.info("matchedTemplate : {}", image.path("matchedTemplate").asText());
    
                if (!"SUCCESS".equals(image.path("inferResult").asText())) {
                    log.warn("OCR 인식 실패: {}", image.path("message").asText());
                    continue;
                }
    
                JsonNode fields = image.path("fields");
                if (fields.isArray() && fields.size() > 0) {
                    log.info("추출된 필드 개수: {}", fields.size());
                    for (JsonNode field : fields) {
                        String text = field.path("inferText").asText();
                        float confidence = field.path("inferConfidence").floatValue();
                        log.info("추출된 텍스트: {} (신뢰도: {})", text, confidence);
                        result.append(text).append(" ");
                    }
                } else {
                    log.warn("fields 항목이 비어있거나 존재하지 않습니다.");
                }
            }
        } else {
            log.warn("OCR 응답에서 images 배열이 존재하지 않습니다.");
        }
    
        log.info("=== 최종 추출된 텍스트 ===");
        log.info(result.toString());
        return result.toString().trim();
    }
    
}
