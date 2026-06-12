package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberInviteRequest {
    @NotBlank(message = "초대할 사용자의 이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    private String userEmail;
    
    private String email;
    @Builder.Default
    private String role = "MEMBER"; // 기본값은 일반 멤버
}
