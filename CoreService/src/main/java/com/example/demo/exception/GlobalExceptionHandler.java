package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<String>> handleValidation(MethodArgumentNotValidException ex) {
        var field = ex.getBindingResult().getFieldError();
        return error(ApiErrorCode.BAD_REQUEST, field == null ? null : field.getDefaultMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<String>> handleUnauthorized(UnauthorizedException ex) {
        return error(ApiErrorCode.AUTH_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler({NotFoundException.class, NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<String>> handleNotFound(Exception ex) {
        return error(ApiErrorCode.NOT_FOUND, ex instanceof NotFoundException ? ex.getMessage() : null);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<String>> handleForbidden(Exception ex) {
        return error(ApiErrorCode.FORBIDDEN, ex instanceof ForbiddenException ? ex.getMessage() : null);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class, BindException.class, HandlerMethodValidationException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<String>> handleBadRequest(Exception ex) {
        return error(ApiErrorCode.BAD_REQUEST, ex.getClass() == IllegalArgumentException.class ? ex.getMessage() : null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        HttpHeaders headers = new HttpHeaders();
        var supportedMethods = ex.getSupportedHttpMethods();
        if (supportedMethods != null) headers.setAllow(supportedMethods);
        var response = error(ApiErrorCode.METHOD_NOT_ALLOWED, null);
        return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<String>> handleUpload(Exception ex) {
        return error(ApiErrorCode.PAYLOAD_TOO_LARGE, null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMediaType(Exception ex) {
        return error(ApiErrorCode.UNSUPPORTED_MEDIA_TYPE, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<String>> handleStatus(ResponseStatusException ex) {
        return error(ApiErrorCode.fromStatus(ex.getStatusCode().value()), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneric(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            return error(ApiErrorCode.fromStatus(errorResponse.getStatusCode().value()), null);
        }
        log.error("처리되지 않은 예외", ex);
        return error(ApiErrorCode.INTERNAL_ERROR, null);
    }

    private ResponseEntity<ApiResponse<String>> error(ApiErrorCode code, String message) {
        String safe = code.fixedMessage() || message == null || message.isBlank() ? code.message() : message;
        return ResponseEntity.status(code.httpStatus()).body(ApiResponse.error(safe, code.name()));
    }
}
