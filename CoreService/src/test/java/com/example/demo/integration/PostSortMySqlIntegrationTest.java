package com.example.demo.integration;

import com.example.demo.dto.board.PagedPostResponse;
import com.example.demo.dto.board.PostCreateRequest;
import com.example.demo.dto.board.PostSearchRequest;
import com.example.demo.integration.support.MySqlTestDatabase;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.PostService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Tag("mysql")
class PostSortMySqlIntegrationTest {

    private static final String HOST = "host@r03.example.test";
    private static final String MEMBER = "member@r03.example.test";
    private static final String OUTSIDER = "outsider@r03.example.test";
    private MySqlTestDatabase database;
    private PostMapper posts;
    private PostService service;

    @BeforeEach
    void setUp() throws Exception {
        database = MySqlTestDatabase.create();
        database.seedFixture();
        posts = database.sqlSession().getMapper(PostMapper.class);
        service = new PostService(posts,
                database.sqlSession().getMapper(BoardMapper.class),
                database.sqlSession().getMapper(BoardMemberMapper.class),
                mock(FileStorageService.class));
        insertPosts();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void serviceNormalizesDirectionsUsesRealSortingAndHonorsHostAndMemberAccess() {
        PagedPostResponse ascending = service.searchPostsWithFilters(HOST, request("viewCount", "  aSc ", 0, 3));
        PagedPostResponse defaultDescending = service.searchPostsWithFilters(MEMBER, request("viewCount", null, 0, 3));

        assertEquals("ASC", ascending.getSortDirection());
        assertEquals(List.of("alpha", "bravo", "charlie"), titles(ascending));
        assertEquals("DESC", defaultDescending.getSortDirection());
        assertEquals(List.of("charlie", "bravo", "alpha"), titles(defaultDescending));
        assertThrows(IllegalArgumentException.class,
                () -> service.searchPostsWithFilters(OUTSIDER, request("viewCount", "DESC", 0, 3)));
    }

    @Test
    void serviceUsesEachAllowedColumnAndDirectionAgainstMySql() {
        assertSorted("createdAt", List.of("alpha", "bravo", "charlie"), List.of("charlie", "bravo", "alpha"));
        assertSorted("viewCount", List.of("alpha", "bravo", "charlie"), List.of("charlie", "bravo", "alpha"));
        assertSorted("likeCount", List.of("charlie", "bravo", "alpha"), List.of("alpha", "bravo", "charlie"));
        assertSorted("commentCount", List.of("alpha", "charlie", "bravo"), List.of("bravo", "charlie", "alpha"));
    }

    @Test
    void serviceFiltersAndPagesWithExplicitEmptyAndPastEndBoundaries() {
        PagedPostResponse blankKeyword = service.searchPostsWithFilters(HOST,
                PostSearchRequest.builder().boardId(400L).keyword("").sortBy("createdAt")
                        .sortDirection("ASC").page(0).size(2).build());
        PagedPostResponse filtered = service.searchPostsWithFilters(HOST,
                PostSearchRequest.builder().boardId(400L).keyword("alpha").searchField("title")
                        .sortBy("createdAt").sortDirection("DESC").page(0).size(10).build());
        PagedPostResponse missing = service.searchPostsWithFilters(HOST,
                PostSearchRequest.builder().boardId(400L).keyword("does-not-exist").searchField("title")
                        .sortBy("createdAt").sortDirection("DESC").page(0).size(10).build());
        PagedPostResponse middlePage = service.searchPostsWithFilters(HOST, request("viewCount", "DESC", 1, 1));
        PagedPostResponse pastEnd = service.searchPostsWithFilters(HOST, request("viewCount", "DESC", 3, 1));

        assertEquals(List.of("alpha", "bravo"), titles(blankKeyword));
        assertEquals(3, blankKeyword.getTotalElements());
        assertEquals(2, blankKeyword.getTotalPages());
        assertTrue(blankKeyword.isFirst());
        assertFalse(blankKeyword.isLast());
        assertEquals(List.of("alpha"), titles(filtered));
        assertEquals(1, filtered.getTotalElements());
        assertTrue(missing.getContent().isEmpty());
        assertEquals(0, missing.getTotalElements());
        assertEquals(0, missing.getTotalPages());
        assertTrue(missing.isFirst());
        assertTrue(missing.isLast());
        assertEquals(List.of("bravo"), titles(middlePage));
        assertEquals(3, middlePage.getTotalPages());
        assertFalse(middlePage.isFirst());
        assertFalse(middlePage.isLast());
        assertTrue(pastEnd.getContent().isEmpty());
        assertTrue(pastEnd.isLast());
    }

    @Test
    void serviceRejectsMaliciousDirectionThenAllowsNormalQueryWithoutChangingRows() {
        int before = database.jdbc().queryForObject("SELECT COUNT(*) FROM board_posts", Integer.class);
        assertThrows(IllegalArgumentException.class,
                () -> service.searchPostsWithFilters(HOST, request("createdAt", "DESC; DROP TABLE board_posts", 0, 10)));
        assertEquals(before, database.jdbc().queryForObject("SELECT COUNT(*) FROM board_posts", Integer.class));
        assertEquals(List.of("charlie", "bravo", "alpha"),
                titles(service.searchPostsWithFilters(HOST, request("viewCount", "DESC", 0, 3))));
    }

    @Test
    void directMapperUsesFixedDescForUnsafeDirection() {
        assertEquals(List.of("charlie", "bravo", "alpha"), posts.findPostsWithFilters(
                400L, null, null, null, null, "viewCount", "DESC; DROP TABLE board_posts", 0, 3)
                .stream().map(post -> post.getTitle()).toList());
        assertEquals(3, database.jdbc().queryForObject("SELECT COUNT(*) FROM board_posts", Integer.class));
    }

    @Test
    void createPostAllowsHostAndActiveMemberAndRejectsNonMember() {
        assertNotNull(service.createPost(HOST, new PostCreateRequest(400L, "host post", "body", List.of())));
        assertNotNull(service.createPost(MEMBER, new PostCreateRequest(400L, "member post", "body", List.of())));
        assertThrows(IllegalArgumentException.class,
                () -> service.createPost(OUTSIDER, new PostCreateRequest(400L, "outsider post", "body", List.of())));
    }

    private void insertPosts() {
        database.jdbc().batchUpdate("INSERT INTO board_posts (board_id, author_email, title, content, created_at, updated_at, is_deleted, view_count, like_count, comment_count) VALUES (?, ?, ?, ?, ?, ?, false, ?, ?, ?)",
                List.of(
                        new Object[]{400L, HOST, "alpha", "alpha body", "2026-01-01 10:00:00", "2026-01-01 10:00:00", 1, 9, 2},
                        new Object[]{400L, MEMBER, "bravo", "bravo body", "2026-01-02 10:00:00", "2026-01-02 10:00:00", 2, 5, 7},
                        new Object[]{400L, HOST, "charlie", "charlie body", "2026-01-03 10:00:00", "2026-01-03 10:00:00", 3, 1, 4}));
    }

    private static PostSearchRequest request(String sortBy, String direction, int page, int size) {
        return PostSearchRequest.builder().boardId(400L).sortBy(sortBy).sortDirection(direction).page(page).size(size).build();
    }

    private static List<String> titles(PagedPostResponse response) {
        return response.getContent().stream().map(post -> post.getTitle()).toList();
    }

    private void assertSorted(String sortBy, List<String> ascending, List<String> descending) {
        assertEquals(ascending, titles(service.searchPostsWithFilters(HOST, request(sortBy, "ASC", 0, 3))));
        assertEquals(descending, titles(service.searchPostsWithFilters(HOST, request(sortBy, "DESC", 0, 3))));
    }
}
