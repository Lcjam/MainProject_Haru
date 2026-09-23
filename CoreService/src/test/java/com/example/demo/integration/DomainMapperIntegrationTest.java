package com.example.demo.integration;

import com.example.demo.integration.support.MySqlTestDatabase;
import com.example.demo.mapper.ChatRoomMapper;
import com.example.demo.mapper.Market.ProductMapper;
import com.example.demo.mapper.Market.ProductRequestMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.model.board.Post;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("mysql")
class DomainMapperIntegrationTest {

    private static final String HOST = "host@r03.example.test";
    private static final String MEMBER = "member@r03.example.test";
    private static final String OUTSIDER = "outsider@r03.example.test";
    private MySqlTestDatabase database;

    @BeforeEach
    void setUp() throws Exception {
        database = MySqlTestDatabase.create();
        database.seedFixture();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (database != null) {
            database.close();
        }
    }

    @Test
    void fixtureMappers_followForeignKeysAndReturnNullForMissingRows() {
        UserMapper users = database.sqlSession().getMapper(UserMapper.class);
        ProductMapper products = database.sqlSession().getMapper(ProductMapper.class);
        ProductRequestMapper requests = database.sqlSession().getMapper(ProductRequestMapper.class);
        BoardMapper boards = database.sqlSession().getMapper(BoardMapper.class);

        assertEquals(HOST, users.findByEmail(HOST).getEmail());
        assertNull(users.findByEmail("missing@r03.example.test"));
        assertEquals(HOST, products.findById(100L, HOST).getEmail());
        assertEquals(200L, requests.findRequestId(100L, MEMBER));
        assertEquals(400L, boards.findBoardById(400L).getId());
        assertNull(boards.findBoardById(999_999L));
    }

    @Test
    void chatRoomQueries_allowSellerAndBuyerButRejectOutsider() {
        ChatRoomMapper rooms = database.sqlSession().getMapper(ChatRoomMapper.class);

        assertEquals(HOST, rooms.findChatRoomById(300, HOST).getSellerEmail());
        assertEquals(MEMBER, rooms.findChatRoomById(300, MEMBER).getRequestEmail());
        assertNull(rooms.findChatRoomById(300, OUTSIDER));
        assertEquals(300, rooms.findChatRoomByProductIdAndEmail(100L, HOST).getChatroomId());
        assertEquals(300, rooms.findChatRoomByProductIdAndEmail(100L, MEMBER).getChatroomId());
        assertNull(rooms.findChatRoomByProductIdAndEmail(100L, OUTSIDER));
    }

    @Test
    void transactionCommitsValidMapperWriteAndRollsBackEarlierWriteOnForeignKeyFailure() {
        PostMapper posts = database.sqlSession().getMapper(PostMapper.class);

        database.transactions().executeWithoutResult(status -> posts.createPost(post(400L, HOST, "committed")));
        assertEquals(1, database.jdbc().queryForObject(
                "SELECT COUNT(*) FROM board_posts WHERE title = 'committed'", Integer.class));

        assertThrows(DataIntegrityViolationException.class, () -> database.transactions().executeWithoutResult(status -> {
            posts.createPost(post(400L, HOST, "rolled-back"));
            posts.createPost(post(400L, "absent@r03.example.test", "invalid-fk"));
        }));
        assertEquals(0, database.jdbc().queryForObject(
                "SELECT COUNT(*) FROM board_posts WHERE title = 'rolled-back'", Integer.class));
    }

    private static Post post(Long boardId, String author, String title) {
        return Post.builder().boardId(boardId).authorEmail(author).title(title).content("content").build();
    }
}
