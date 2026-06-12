// API 응답 구조 통일을 위한 공통 클래스
package com.example.demo.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {
    private String status;  // 상태 필드 (success / error)
    private String message; // 메시지 필드
    private T data;

    // 기본 생성자 (성공 응답)
    public BaseResponse(T data) {
        this.status = "success";
        this.message = "요청이 성공적으로 처리되었습니다.";
        this.data = data;
    }

    // 커스텀 메시지를 포함한 성공 응답
    public BaseResponse(T data, String message) {
        this.status = "success";
        this.message = message;
        this.data = data;
    }

    // 정적 성공 응답 메서드 추가 (data만)
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(data, "요청이 성공적으로 처리되었습니다.");
    }

    // 정적 성공 응답 메서드 추가 (data + custom message)
    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(data, message);
    }

    // 에러 응답 (명확한 타입 지정)
    public static <T> BaseResponse<T> error(String message) {
        BaseResponse<T> response = new BaseResponse<>(null);
        response.status = "error";
        response.message = message;
        return response;
    }

    // Getter 추가
    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
