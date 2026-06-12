package com.example.demo.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    private String isToken;
    private String email;
    private String phoneNumber;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
