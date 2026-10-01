package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    private String status;
    private Data data;
    private String code;
    private String message;

    public ChatResponseDTO(String status, String errorMessage, String code) {
        this.status = status;
        this.data = new Data();
        this.data.setResponse(errorMessage);
        this.code = code;
        this.message = errorMessage;
    }

    @lombok.Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        private String message;
        private String response;
    }
}
