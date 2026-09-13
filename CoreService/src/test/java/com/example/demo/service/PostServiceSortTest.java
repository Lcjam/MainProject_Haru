package com.example.demo.service;

import com.example.demo.dto.board.PagedPostResponse;
import com.example.demo.dto.board.PostSearchRequest;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.model.board.Board;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("PostService 게시글 정렬 방향 검증")
class PostServiceSortTest {

    private final PostMapper postMapper = mock(PostMapper.class);
    private final BoardMapper boardMapper = mock(BoardMapper.class);
    private final BoardMemberMapper boardMemberMapper = mock(BoardMemberMapper.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final PostService postService = new PostService(
            postMapper, boardMapper, boardMemberMapper, fileStorageService);

    @ParameterizedTest(name = "입력 {0}은 {1}로 전달된다")
    @MethodSource("validSortDirections")
    @DisplayName("허용된 방향을 정규화하고 빈값은 DESC로 기본화한다")
    void searchPostsWithFilters_normalizesAllowedDirection(String input, String expected) {
        PostSearchRequest request = requestWithDirection(input);
        given(boardMapper.findBoardById(1L)).willReturn(hostedBoard());
        given(postMapper.findPostsWithFilters(
                eq(1L), any(), any(), any(), any(), eq("viewCount"), eq(expected), eq(0), eq(10)))
                .willReturn(List.of());
        given(postMapper.countPostsWithFilters(eq(1L), any(), any(), any(), any())).willReturn(0);

        PagedPostResponse response = postService.searchPostsWithFilters("host@haru.com", request);

        assertEquals(expected, response.getSortDirection());
        verify(postMapper).findPostsWithFilters(
                eq(1L), any(), any(), any(), any(), eq("viewCount"), eq(expected), eq(0), eq(10));
    }

    @ParameterizedTest(name = "거부 입력: {0}")
    @MethodSource("invalidSortDirections")
    @DisplayName("허용되지 않은 정렬 방향은 SQL 실행 전에 거부한다")
    void searchPostsWithFilters_rejectsUnsafeDirection(String input) {
        PostSearchRequest request = requestWithDirection(input);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> postService.searchPostsWithFilters("host@haru.com", request));

        assertEquals("정렬 방향은 ASC 또는 DESC만 사용할 수 있습니다.", exception.getMessage());
        verifyNoInteractions(boardMapper, postMapper);
    }

    private static Stream<Arguments> validSortDirections() {
        return Stream.of(
                Arguments.of(null, "DESC"),
                Arguments.of("", "DESC"),
                Arguments.of("   ", "DESC"),
                Arguments.of("ASC", "ASC"),
                Arguments.of("DESC", "DESC"),
                Arguments.of("  aSc  ", "ASC"),
                Arguments.of("  dEsC  ", "DESC"));
    }

    private static Stream<String> invalidSortDirections() {
        return Stream.of(
                "sideways",
                "DESC; DROP TABLE board_posts",
                "ASC --",
                "DESC /* injected */");
    }

    private PostSearchRequest requestWithDirection(String sortDirection) {
        return PostSearchRequest.builder()
                .boardId(1L)
                .sortBy("viewCount")
                .sortDirection(sortDirection)
                .build();
    }

    private Board hostedBoard() {
        return Board.builder().id(1L).hostEmail("host@haru.com").build();
    }
}
