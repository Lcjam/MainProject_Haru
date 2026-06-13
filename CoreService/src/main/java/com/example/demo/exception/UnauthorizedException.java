package com.example.demo.exception;

/**
 * 인증되지 않은 요청(유효하지 않은/누락된 토큰)을 표현한다.
 * 컨트롤러마다 반복되던 {@code email == null → 401} 가드를
 * {@link GlobalExceptionHandler} 로 위임하기 위한 예외.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
