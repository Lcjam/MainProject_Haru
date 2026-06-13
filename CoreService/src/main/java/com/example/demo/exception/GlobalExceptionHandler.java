package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 컨트롤러에서 처리되지 않고 전파된 예외를 일관된 {@link ApiResponse} 형식(UTF-8 JSON)으로 변환한다.
 * 각 컨트롤러가 자체 try/catch 로 반환하는 응답에는 영향을 주지 않으며, 미처리 예외만 표준화한다.
 * (이전: @Valid 검증 실패가 security entry point 로 흘러 401 + 깨진 한글로 반환되던 문제를 교정)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** @Valid 검증 실패 → 400 + 첫 번째 검증 메시지 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidation(MethodArgumentNotValidException ex) {
        FieldError fieldError = ex.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "잘못된 요청입니다.";
        log.debug("검증 실패: {}", message);
        return ResponseEntity.badRequest().body(ApiResponse.error(message, "400"));
    }

    /** 인증 실패(유효하지 않은/누락된 토큰) → 401 */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorized(UnauthorizedException ex) {
        log.debug("인증 실패: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage(), "401"));
    }

    /** 잘못된 인자 → 400 */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("잘못된 인자: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), "400"));
    }

    /** 그 외 미처리 예외 → 500 (스택은 로깅, 본문엔 일반 메시지) */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneric(Exception ex) {
        log.error("처리되지 않은 예외", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
    }
}
