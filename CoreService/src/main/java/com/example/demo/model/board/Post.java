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
public class Post {
    private Long id;
    private Long boardId;
    private String authorEmail;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    
    // 게시판 조인 필드
    private String boardName;
    
    // 작성자 조인 필드
    private String authorName;
    private String authorNickname;
    private String authorProfileImage;
}
