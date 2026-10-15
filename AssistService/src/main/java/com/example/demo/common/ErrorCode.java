package com.example.demo.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다.", false),
    INVALID_PATH(HttpStatus.BAD_REQUEST, "잘못된 요청 경로입니다.", true),
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", false),
    AUTH_EXPIRED(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다.", true),
    AUTH_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 인증입니다.", false),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", false),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 자원을 찾을 수 없습니다.", false),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청 방식입니다.", true),
    CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌합니다.", false),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.", false),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다.", false),
    DUPLICATE_PHONE(HttpStatus.CONFLICT, "이미 등록된 전화번호입니다.", false),
    PAYLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "요청 크기가 너무 큽니다.", true),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다.", true),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", true),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.", true),
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "서비스에 일시적으로 연결할 수 없습니다.", true),
    UPSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "서비스 응답이 지연되고 있습니다.", true);

    private final HttpStatus httpStatus;
    private final String defaultMessage;
    private final boolean fixedMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage, boolean fixedMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
        this.fixedMessage = fixedMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getStatus() {
        return httpStatus.value();
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public boolean isFixedMessage() {
        return fixedMessage;
    }

    public static ErrorCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return INTERNAL_ERROR;
        }
        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException e) {
            return switch (code) {
                case "400" -> BAD_REQUEST;
                case "401" -> AUTH_REQUIRED;
                case "403" -> FORBIDDEN;
                case "404" -> NOT_FOUND;
                case "405" -> METHOD_NOT_ALLOWED;
                case "409" -> CONFLICT;
                case "413" -> PAYLOAD_TOO_LARGE;
                case "415" -> UNSUPPORTED_MEDIA_TYPE;
                case "429" -> TOO_MANY_REQUESTS;
                case "500" -> INTERNAL_ERROR;
                case "502", "503" -> UPSTREAM_UNAVAILABLE;
                case "504" -> UPSTREAM_TIMEOUT;
                default -> INTERNAL_ERROR;
            };
        }
    }

    public static ErrorCode fromStatus(int status) {
        return switch (status) {
            case 400 -> BAD_REQUEST;
            case 401 -> AUTH_REQUIRED;
            case 403 -> FORBIDDEN;
            case 404 -> NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> CONFLICT;
            case 413 -> PAYLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> TOO_MANY_REQUESTS;
            case 502, 503 -> UPSTREAM_UNAVAILABLE;
            case 504 -> UPSTREAM_TIMEOUT;
            default -> INTERNAL_ERROR;
        };
    }
}
