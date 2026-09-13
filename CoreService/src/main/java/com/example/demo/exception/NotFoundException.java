package com.example.demo.exception;

/**
 * 요청한 자원이 존재하지 않음을 표현한다.
 * 컨트롤러마다 반복되던 {@code null 검사 → 404} 분기를
 * {@link GlobalExceptionHandler} 로 위임하기 위한 예외.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
