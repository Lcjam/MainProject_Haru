package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assist/location")
public class EndLocationController {
    
    @Value("${kakao.rest.api.key}")
    private String kakaoRestApiKey;    
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    @GetMapping("/search")
    public ResponseEntity<?> searchAddress(@RequestParam String query) {
        try {
            
         
            
            String kakaoUrl = "https://dapi.kakao.com/v2/local/search/keyword.json"
                + "?query=" + query
                + "&size=5";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            log.info("Requesting Kakao API with URL: {}", kakaoUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                kakaoUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("Kakao API Response Status: {}", response.getStatusCode());
            log.info("Kakao API Response Body: {}", response.getBody());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
                
        } catch (HttpClientErrorException e) {
            log.error("Kakao API Error: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                    "카카오 API 호출 중 오류가 발생했습니다.",
                    e.getResponseBodyAsString()
                ));
        } catch (Exception e) {
            log.error("주소 검색 중 예외 발생: ", e);
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                    "주소 검색 중 오류가 발생했습니다.",
                    e.getMessage()
                ));
        }
    }
    // AssistService/src/main/java/com/example/demo/controller/EndLocationController.java
    @GetMapping("/coord-to-address")
    public ResponseEntity<?> coordToAddress(
        @RequestParam("lat") double lat,
        @RequestParam("lng") double lng
    ) {
        try {
            log.info("Received coordinate to address request - lat: {}, lng: {}", lat, lng);
            
            // URL 인코딩 처리
            String kakaoUrl = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/geo/coord2address.json")
                .queryParam("x", String.valueOf(lng))
                .queryParam("y", String.valueOf(lat))
                .queryParam("input_coord", "WGS84")
                .encode()
                .toUriString();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            
            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            log.info("Requesting Kakao API with URL: {}", kakaoUrl);
            
            ResponseEntity<String> response = restTemplate.exchange(
                kakaoUrl,
                HttpMethod.GET,
                entity,
                String.class
            );
            
            log.info("Kakao API Response Status: {}", response.getStatusCode());
            log.debug("Kakao API Response Body: {}", response.getBody());
            
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
                
        } catch (HttpClientErrorException e) {
            log.error("Kakao API Error: {}", e.getMessage());
            return ResponseEntity
                .status(e.getStatusCode())
                .body(new ErrorResponse(
                    "카카오 API 호출 중 오류가 발생했습니다.",
                    e.getResponseBodyAsString()
                ));
        } catch (IllegalArgumentException e) {
            log.error("Invalid coordinate values: {}", e.getMessage());
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                    "잘못된 좌표값이 입력되었습니다.",
                    e.getMessage()
                ));
        } catch (Exception e) {
            log.error("좌표-주소 변환 중 예외 발생: ", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                    "좌표-주소 변환 중 서버 오류가 발생했습니다.",
                    e.getMessage()
                ));
        }
    }
}

class ErrorResponse {
    private final String message;
    private final String details;
    
    public ErrorResponse(String message, String details) {
        this.message = message;
        this.details = details;
    }
    
    public String getMessage() { return message; }
    public String getDetails() { return details; }
}
