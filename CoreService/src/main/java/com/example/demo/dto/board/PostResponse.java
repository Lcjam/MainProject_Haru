package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private Long id;
    private Long boardId;
    private String boardName;
    private String authorEmail;
    private String authorName;
    private String authorNickname;
    private String authorProfileImage;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private List<String> imageUrls;
    private boolean isAuthor; // 현재 사용자가 작성자인지 여부
}
