package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchRequest {
    private Long boardId;                    // 게시판 ID
    private String keyword;                  // 검색 키워드
    private String searchField;              // 검색 필드 (title, content, author)
    private LocalDateTime startDate;         // 검색 시작 날짜
    private LocalDateTime endDate;           // 검색 종료 날짜
    private String sortBy;                   // 정렬 기준 (createdAt, viewCount, likeCount, commentCount)
    private String sortDirection;            // 정렬 방향 (ASC, DESC)
    private Integer page;                    // 페이지 번호 (0부터 시작)
    private Integer size;                    // 페이지 크기
}
