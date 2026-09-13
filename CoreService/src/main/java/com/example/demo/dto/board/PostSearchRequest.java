package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostSearchRequest {
    private static final String DEFAULT_SORT_DIRECTION = "DESC";

    private Long boardId;                    // 게시판 ID
    private String keyword;                  // 검색 키워드
    private String searchField;              // 검색 필드 (title, content, author)
    private LocalDateTime startDate;         // 검색 시작 날짜
    private LocalDateTime endDate;           // 검색 종료 날짜
    private String sortBy;                   // 정렬 기준 (createdAt, viewCount, likeCount, commentCount)
    private String sortDirection;            // 정렬 방향 (ASC, DESC)
    private Integer page;                    // 페이지 번호 (0부터 시작)
    private Integer size;                    // 페이지 크기

    /**
     * 정렬 방향을 SQL에서 사용할 수 있는 고정 값으로 변환한다.
     * null 또는 공백은 DESC를 사용하고, 주변 공백과 대소문자 차이를 허용한 ASC/DESC 외 값은 거부한다.
     */
    public String resolveSortDirection() {
        if (sortDirection == null || sortDirection.isBlank()) {
            return DEFAULT_SORT_DIRECTION;
        }

        String normalizedDirection = sortDirection.trim().toUpperCase(Locale.ROOT);
        if ("ASC".equals(normalizedDirection) || "DESC".equals(normalizedDirection)) {
            return normalizedDirection;
        }

        throw new IllegalArgumentException("정렬 방향은 ASC 또는 DESC만 사용할 수 있습니다.");
    }
}
