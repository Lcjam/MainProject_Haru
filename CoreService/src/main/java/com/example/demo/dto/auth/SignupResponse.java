package com.example.demo.dto.auth;

import com.example.demo.exception.ApiErrorCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private boolean success;
    @JsonIgnore
    private ApiErrorCode errorCode;
    private String email;
    private String message;
    private Integer initialDopamine; // 초기 도파민 수치
    private Integer initialPoints;   // 초기 활동 포인트 추가
}
