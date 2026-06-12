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
public class PostReaction {
    private Long id;
    private Long postId;
    private String userEmail;
    private String reactionType; // LIKE, LOVE, HAHA, WOW, SAD, ANGRY 등
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 조인으로 가져올 추가 정보
    private String userName;
    private String userNickname;
    private String userProfileImage;
}
