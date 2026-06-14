package com.example.demo.controller;
import lombok.extern.slf4j.Slf4j;

import com.example.demo.dto.ChatRequestDTO;
import com.example.demo.dto.ChatResponseDTO;
import com.example.demo.model.ChatMessage;
import com.example.demo.serviceimpl.LlamaServiceImpl;
import com.example.demo.dao.ChatMessageDAO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;
import com.example.demo.util.TokenUtils;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assist/tinylamanaver")
@Tag(name = "Tiny Llama Chat API", description = "LlamaService 기반 AI 채팅 API")
@Slf4j
public class LlamaController {

    private final LlamaServiceImpl llamaServiceImpl;
    private final ChatMessageDAO chatMessageDAO;
    private final TokenUtils tokenUtils;

    @Operation(summary = "AI 챗봇과 대화")
    @PostMapping("/chat")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request, @RequestHeader("Authorization") String token) {
        try {
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ChatResponseDTO("error", "메시지 내용이 비어있습니다.", "400"));
            }
            
            log.debug("=== 채팅 요청 수신 ===");
            log.debug("수신된 토큰: " + token);

            // 토큰에서 이메일 추출 (Bearer 제거)
            String userEmail = tokenUtils.getEmailFromToken(tokenUtils.extractTokenWithoutBearer(token));
            
            log.debug("추출된 이메일: " + userEmail);
            if (userEmail == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ChatResponseDTO("error", "유효하지 않은 토큰입니다.", "401"));
            }

            String message = request.getMessage();
            // 사용자 이메일을 세션 ID로 사용
            String sessionId = userEmail;

            // DB에서 히스토리 가져오기
            List<ChatMessage> history = chatMessageDAO.getMessagesBySessionId(sessionId);

            // LlamaService 호출 시 히스토리 전달
            String response = llamaServiceImpl.chat(message, sessionId, history);

            ChatResponseDTO.Data data = new ChatResponseDTO.Data();
            data.setMessage(message);
            data.setResponse(response);
            
            ChatResponseDTO responseDTO = new ChatResponseDTO();
            responseDTO.setStatus("success");
            responseDTO.setData(data);
            responseDTO.setCode("200");

            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ChatResponseDTO("error", "서버 처리 중 오류가 발생했습니다.", "500"));
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponseDTO> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ChatResponseDTO("error", "예상치 못한 오류가 발생했습니다.", "500"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ChatResponseDTO> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ChatResponseDTO("error", "잘못된 요청 형식입니다.", "400"));
    }
}
