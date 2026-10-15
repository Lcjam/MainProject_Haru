package com.example.demo.dto;

import com.example.demo.common.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponseDTO<T> {
    private String status;
    private Data<T> data;
    private String code;
    private String message;

    public static <T> CommonResponseDTO<T> error(ErrorCode errorCode) {
        return CommonResponseDTO.<T>builder()
                .status("error")
                .code(errorCode.name())
                .message(errorCode.getDefaultMessage())
                .build();
    }

    public static <T> CommonResponseDTO<T> error(ErrorCode errorCode, String message) {
        return CommonResponseDTO.<T>builder()
                .status("error")
                .code(errorCode.name())
                .message(errorCode.isFixedMessage() || message == null || message.isBlank() ? errorCode.getDefaultMessage() : message)
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data<T> {
        private String message;
        private T response;
    }
}
