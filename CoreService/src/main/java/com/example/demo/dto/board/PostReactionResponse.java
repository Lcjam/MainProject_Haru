package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostReactionResponse {
    private Long postId;
    private String userEmail;
    private String reactionType;
    private LocalDateTime createdAt;
    private boolean isReacted; // 현재 사용자의 반응 여부
    private Map<String, Integer> reactionCounts; // 각 반응 유형별 개수
}
