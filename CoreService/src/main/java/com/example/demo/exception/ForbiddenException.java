package com.example.demo.exception;

/**
 * 인증은 되었으나 해당 자원에 대한 권한이 없음을 표현한다.
 * 컨트롤러마다 반복되던 {@code 소유자/참가자 불일치 → 403} 분기를
 * {@link GlobalExceptionHandler} 로 위임하기 위한 예외.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
