package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String status;
    private T data;
    private String code;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .code("200")
                .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String code) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .code(code)
                .build();
    }
    
    public static <T> ApiResponse<T> error(T data, String code) {
        return ApiResponse.<T>builder()
                .status("error")
                .data(data)
                .code(code)
                .build();
    }
}
