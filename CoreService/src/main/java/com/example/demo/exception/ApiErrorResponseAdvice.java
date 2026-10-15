package com.example.demo.exception;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.util.BaseResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiErrorResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper mapper;

    public ApiErrorResponseAdvice(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter method, MediaType mediaType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        int status = response instanceof ServletServerHttpResponse servlet ? servlet.getServletResponse().getStatus() : 200;
        Object normalized = normalize(body, status);
        if (normalized instanceof ApiResponse<?> error && "error".equals(error.getStatus())) {
            response.setStatusCode(org.springframework.http.HttpStatusCode.valueOf(ApiErrorCode.resolve(error.getCode(), status).httpStatus()));
        }
        return normalized;
    }

    public Object normalize(Object body, int status) {
        String code;
        String message;
        Object data;
        if (body instanceof ApiResponse<?> api) {
            if (!"error".equals(api.getStatus()) && status < 400) return body;
            code = api.getCode(); message = api.getMessage(); data = api.getData();
        } else if (body instanceof BaseResponse<?> base) {
            if (!"error".equals(base.getStatus()) && status < 400) return body;
            code = null; message = base.getMessage(); data = base.getData();
        } else {
            return body;
        }
        ApiErrorCode error = ApiErrorCode.resolve(code, status >= 400 ? status : 400);
        if (error.fixedMessage()) {
            if (data instanceof String) data = error.message();
            // Legacy 500 DTOs may contain the same exception in a nested message.
            else if (error == ApiErrorCode.INTERNAL_ERROR) data = null;
            message = error.message();
        } else if (message == null || message.isBlank()) {
            JsonNode legacy = mapper.valueToTree(data);
            JsonNode nestedMessage = legacy == null ? null : legacy.get("message");
            message = data instanceof String text ? text
                    : nestedMessage != null && nestedMessage.isTextual() ? nestedMessage.asText() : null;
            if (message == null || message.isBlank()) message = error.message();
        }
        return new ApiResponse<>("error", data, error.name(), message);
    }
}
