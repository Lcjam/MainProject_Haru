package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;  // PK (BIGINT)
    private String email;  // 사용자 이메일 (UNIQUE)
    private String passwordHash;  // 비밀번호 해시
    private String name;  // 이름
    private String phoneNumber;
    private String nickname;  // 닉네임 (UNIQUE)
    private String profileImagePath;  // 프로필 이미지 경로 (추가)
    private String bio;
    private String loginMethod;  // 로그인 방식 (EMAIL, SOCIAL)
    private String socialProvider;  // 소셜 로그인 제공자 (GOOGLE, KAKAO, NAVER, NONE)
    private String accountStatus;  // 계정 상태 (Active, Deactivated, Dormant, Withdrawal)
    private String authority;  // 권한 (USER, ADMIN)
    private LocalDateTime signupDate;  // 가입 일자
    private LocalDateTime lastUpdateDate;  // 정보 수정 일자
    private LocalDateTime lastLoginTime;  // 마지막 로그인 시간
    private Integer loginFailedAttempts;  // 로그인 실패 횟수
    private Boolean loginIsLocked;  // 계정 잠금 여부
}
