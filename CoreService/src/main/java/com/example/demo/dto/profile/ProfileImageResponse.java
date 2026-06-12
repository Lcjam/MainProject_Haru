package com.example.demo.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageResponse {
    private boolean success;
    private String message;
    private String profileImagePath;
    private String profileImageUrl;
}
