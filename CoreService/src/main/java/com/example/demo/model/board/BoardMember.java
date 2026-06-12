package com.example.demo.model.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMember {
    private Long id;
    private Long boardId;
    private String userEmail;
    private String role; // HOST, ADMIN, MEMBER
    private String status; // ACTIVE, PENDING, BANNED
    private LocalDateTime joinedAt;
    private String invitedBy;
    
    // 추가 정보 (조인 결과)
    private String userName;
    private String profileImagePath;
}
