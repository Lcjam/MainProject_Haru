package com.example.demo.dto.profile;

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
public class ProfileUpdateResponse {
    private boolean success;
    @JsonIgnore
    private ApiErrorCode errorCode;
    private String message;
    private ProfileResponse updatedProfile;
}
