package com.example.demo.dto;

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

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data<T> {
        private String message;
        private T response;
    }
}
