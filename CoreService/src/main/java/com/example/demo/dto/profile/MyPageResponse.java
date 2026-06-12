package com.example.demo.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyPageResponse {
    private boolean success;
    private String message;
    
    // 마이페이지에 표시될 최소 정보
    private String name;
    private String nickname;
    private LocalDateTime signupDate;
    private LocalDateTime lastLoginTime;
    private String profileImageUrl;  // 프로필 이미지 URL
    private String accountStatus;    // 계정 상태
    private Integer dopamine;        // 도파민 수치 필드 추가
    private Integer points;          // 활동 포인트 추가
}
