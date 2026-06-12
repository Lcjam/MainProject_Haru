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
public class Comment {
    private Long id;
    private Long postId;
    private Long parentId; // 대댓글의 경우 부모 댓글 ID
    private String authorEmail;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
    
    // JOIN 필드
    private String authorName;
    private String authorNickname;
    private String authorProfileImage;
    private int replyCount; // 대댓글 수
    private int depth; // 댓글 깊이 (0: 일반 댓글, 1: 대댓글)
}
