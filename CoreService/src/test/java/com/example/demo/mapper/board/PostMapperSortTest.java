package com.example.demo.mapper.board;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PostMapper 고급 검색 정렬 SQL")
class PostMapperSortTest {

    private static MappedStatement findPostsWithFilters;

    @BeforeAll
    static void loadMapper() throws IOException {
        Configuration configuration = new Configuration();
        String resource = "mapper/board/PostMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments()).parse();
        }
        findPostsWithFilters = configuration.getMappedStatement(
                "com.example.demo.mapper.board.PostMapper.findPostsWithFilters");
    }

    @Test
    @DisplayName("허용된 ASC와 선택한 정렬 컬럼만 SQL에 렌더링한다")
    void boundSql_rendersSelectedColumnAndAsc() {
        String sql = sqlFor("likeCount", "ASC");

        assertTrue(sql.contains("ORDER BY p.like_count ASC"));
    }

    @Test
    @DisplayName("DESC는 선택한 정렬 컬럼에 고정 SQL로 렌더링한다")
    void boundSql_rendersSelectedColumnAndDesc() {
        String sql = sqlFor("commentCount", "DESC");

        assertTrue(sql.contains("ORDER BY p.comment_count DESC"));
    }

    @Test
    @DisplayName("알 수 없는 정렬 컬럼과 빈 방향은 created_at DESC로 기본화한다")
    void boundSql_defaultsUnknownColumnAndBlankDirection() {
        String sql = sqlFor("unknown", "");

        assertTrue(sql.contains("ORDER BY p.created_at DESC"));
    }

    @Test
    @DisplayName("매퍼 직접 호출의 악성 방향 문자열은 SQL에 포함하지 않는다")
    void boundSql_doesNotInterpolateUnsafeDirection() {
        String unsafeDirection = "DESC; DROP TABLE board_posts";

        String sql = sqlFor("viewCount", unsafeDirection);

        assertTrue(sql.contains("ORDER BY p.view_count DESC"));
        assertFalse(sql.contains(unsafeDirection));
        assertFalse(sql.contains("DROP TABLE"));
    }

    private String sqlFor(String sortBy, String sortDirection) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("boardId", 1L);
        parameters.put("sortBy", sortBy);
        parameters.put("sortDirection", sortDirection);
        parameters.put("offset", 0);
        parameters.put("limit", 10);

        BoundSql boundSql = findPostsWithFilters.getBoundSql(parameters);
        return boundSql.getSql().replaceAll("\\s+", " ").trim();
    }
}
