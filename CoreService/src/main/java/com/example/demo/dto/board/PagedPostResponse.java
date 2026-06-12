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
public class PagedPostResponse {
    private List<PostResponse> content;      // 현재 페이지의 게시글 목록
    private int pageNumber;                  // 현재 페이지 번호
    private int pageSize;                    // 페이지 크기
    private int totalPages;                  // 전체 페이지 수
    private long totalElements;              // 전체 요소 수
    private boolean first;                   // 첫 페이지인지 여부
    private boolean last;                    // 마지막 페이지인지 여부
    private String sortBy;                   // 정렬 기준
    private String sortDirection;            // 정렬 방향
}
