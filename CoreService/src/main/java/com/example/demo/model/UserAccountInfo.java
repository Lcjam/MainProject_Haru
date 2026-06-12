package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccountInfo {
    private String email;  // PK & FK (Users 테이블과 연결)
    private String accountStatus;  // 계정 상태 (Active, Deactivated, Dormant, Withdrawal)
    private String authority;  // 권한 (1 = 일반, 2 = 관리자)
    private String authorityName;  // 권한명 (Regular User, Admin)
}
