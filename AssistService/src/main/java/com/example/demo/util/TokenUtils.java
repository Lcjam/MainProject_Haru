package com.example.demo.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Component
@Slf4j
public class TokenUtils {

    // "Bearer " 접두사만 제거 (전역 replace 아님)
    public String extractTokenWithoutBearer(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    // JWT 토큰에서 이메일 추출 (서명 검증 없이 페이로드 디코딩만 수행)
    public String getEmailFromToken(String token) {
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                log.debug("토큰 형식 오류: 청크가 3개가 아님");
                return null;
            }

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(chunks[1]));
            log.debug("디코딩된 페이로드: {}", payload);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(payload);

            // "email" 대신 "sub" 필드에서 이메일 추출
            String email = node.has("sub") ? node.get("sub").asText() : null;
            log.debug("추출된 이메일(sub): {}", email);

            return email;
        } catch (Exception e) {
            log.debug("토큰 처리 중 오류 발생: {}", e.getMessage());
            return null;
        }
    }
}
