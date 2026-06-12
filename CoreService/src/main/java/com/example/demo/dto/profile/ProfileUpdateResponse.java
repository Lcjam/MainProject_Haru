package com.example.demo.dto.profile;

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
    private String message;
    private ProfileResponse updatedProfile;
}
