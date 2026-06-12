package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentCreateRequest {
    private Long postId;
    private Long parentId; // 대댓글인 경우 부모 댓글 ID, 아니면 null
    private String content;
}
