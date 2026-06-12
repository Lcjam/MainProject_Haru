package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {
    private Long boardId;
    private String title;
    private String content;
    private List<String> imageUrls; // 이미지 URL 목록 (선택)
}
