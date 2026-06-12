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
public class CommentResponse {
    private Long id;
    private Long postId;
    private Long parentId;
    private String authorEmail;
    private String authorName;
    private String authorNickname;
    private String authorProfileImage;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isAuthor; // 현재 사용자가 작성자인지 여부
    private int replyCount; // 대댓글 수
    private int depth; // 댓글 깊이 (0: 일반 댓글, 1: 대댓글)
    private List<CommentResponse> replies; // 대댓글 목록 (사용될 경우)
}
