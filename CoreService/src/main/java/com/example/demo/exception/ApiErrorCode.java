package com.example.demo.exception;

public enum ApiErrorCode {
    BAD_REQUEST(400, "잘못된 요청입니다.", false),
    INVALID_PATH(400, "잘못된 요청 경로입니다.", true),
    AUTH_REQUIRED(401, "로그인이 필요합니다.", false),
    AUTH_EXPIRED(401, "인증이 만료되었습니다.", true),
    AUTH_INVALID(401, "유효하지 않은 인증입니다.", false),
    FORBIDDEN(403, "접근 권한이 없습니다.", false),
    NOT_FOUND(404, "요청한 자원을 찾을 수 없습니다.", false),
    METHOD_NOT_ALLOWED(405, "허용되지 않은 요청 방식입니다.", true),
    CONFLICT(409, "요청이 현재 상태와 충돌합니다.", false),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다.", false),
    DUPLICATE_NICKNAME(409, "이미 사용 중인 닉네임입니다.", false),
    DUPLICATE_PHONE(409, "이미 등록된 전화번호입니다.", false),
    PAYLOAD_TOO_LARGE(413, "요청 크기가 너무 큽니다.", true),
    UNSUPPORTED_MEDIA_TYPE(415, "지원하지 않는 요청 형식입니다.", true),
    TOO_MANY_REQUESTS(429, "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.", true),
    INTERNAL_ERROR(500, "서버 오류가 발생했습니다.", true),
    UPSTREAM_UNAVAILABLE(503, "서비스에 일시적으로 연결할 수 없습니다.", true),
    UPSTREAM_TIMEOUT(504, "서비스 응답이 지연되고 있습니다.", true);

    private final int httpStatus;
    private final String message;
    private final boolean fixedMessage;

    ApiErrorCode(int httpStatus, String message, boolean fixedMessage) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.fixedMessage = fixedMessage;
    }

    public int httpStatus() { return httpStatus; }
    public String message() { return message; }
    public boolean fixedMessage() { return fixedMessage; }

    public static ApiErrorCode fromStatus(int status) {
        if (status == 502) return UPSTREAM_UNAVAILABLE;
        for (ApiErrorCode code : values()) {
            if (code.httpStatus == status) return code;
        }
        return status >= 500 ? INTERNAL_ERROR : BAD_REQUEST;
    }

    public static ApiErrorCode resolve(String value, int status) {
        if (value == null) return fromStatus(status);
        return switch (value) {
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
            case "502" -> UPSTREAM_UNAVAILABLE;
            case "503" -> UPSTREAM_UNAVAILABLE;
            case "504" -> UPSTREAM_TIMEOUT;
            default -> {
                try { yield valueOf(value); }
                catch (IllegalArgumentException ignored) { yield fromStatus(status); }
            }
        };
    }
}
