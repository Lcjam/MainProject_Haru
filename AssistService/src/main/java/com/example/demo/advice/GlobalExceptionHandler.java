package com.example.demo.advice;

import com.example.demo.common.ErrorCode;
import com.example.demo.dto.CommonResponseDTO;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Set;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponseDTO<?>> handleValidation(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldError();
        String message = (field != null) ? field.getDefaultMessage() : null;
        return error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class,
            MultipartException.class,
            MissingRequestHeaderException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            BindException.class
    })
    public ResponseEntity<CommonResponseDTO<?>> handleBadRequest(Exception ex) {
        log.warn("잘못된 요청 예외: {}", ex.getMessage());
        return error(ErrorCode.BAD_REQUEST, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponseDTO<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("지원하지 않는 HTTP 메서드: {}", ex.getMethod());
        var builder = ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON);
        Set<HttpMethod> supportedMethods = ex.getSupportedHttpMethods();
        if (supportedMethods != null && !supportedMethods.isEmpty()) {
            builder.allow(supportedMethods.toArray(HttpMethod[]::new));
        }
        return builder.body(CommonResponseDTO.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CommonResponseDTO<?>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("업로드 크기 초과: {}", ex.getMessage());
        return error(ErrorCode.PAYLOAD_TOO_LARGE, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<CommonResponseDTO<?>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("지원하지 않는 미디어 타입: {}", ex.getMessage());
        return error(ErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<CommonResponseDTO<?>> handleNotFound(Exception ex) {
        log.warn("경로를 찾을 수 없음: {}", ex.getMessage());
        return error(ErrorCode.NOT_FOUND, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<CommonResponseDTO<?>> handleResponseStatus(ResponseStatusException ex) {
        ErrorCode code = ErrorCode.fromStatus(ex.getStatusCode().value());
        return error(code, null);
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<CommonResponseDTO<?>> handleErrorResponseException(ErrorResponseException ex) {
        ErrorCode code = ErrorCode.fromStatus(ex.getStatusCode().value());
        return error(code, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponseDTO<?>> handleGeneralException(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            ErrorCode code = ErrorCode.fromStatus(errorResponse.getStatusCode().value());
            return error(code, null);
        }
        log.error("처리되지 않은 서버 내부 예외 발생", ex);
        return error(ErrorCode.INTERNAL_ERROR, null);
    }

    private ResponseEntity<CommonResponseDTO<?>> error(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(CommonResponseDTO.error(errorCode, message));
    }
}
