package com.example.demo.service;

import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.mapper.board.PostReactionMapper;
import com.example.demo.model.board.Post;
import com.example.demo.model.board.PostReaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 단위 1a-3 — PostReactionController 가 직접 쓰던 PostReactionMapper/PostMapper 호출을
 * PostReactionService 로 옮긴 뒤의 신규 메서드(applyReaction/getUserReaction/deleteReactionAndSync/
 * togglePostLike) 단위 테스트.
 *
 * 주의: 이 메서드들은 기존 reactToPost(:37)/removeReaction(:108) 과 이름이 비슷해 보이지만 별개다 —
 * 그 두 메서드는 호출부 0의 죽은 코드이고 게시판 멤버십 검증까지 포함하지만, 컨트롤러의 현행 동작은
 * 멤버십 검증이 없다. 여기서 검증하는 신규 메서드는 컨트롤러의 현행 로직을 그대로 옮긴 것이다.
 */
@DisplayName("PostReactionService 신규 메서드 (Mapper 직접 주입 제거분)")
class PostReactionServiceTest {

    private final PostReactionMapper postReactionMapper = mock(PostReactionMapper.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final BoardMapper boardMapper = mock(BoardMapper.class);
    private final BoardMemberMapper boardMemberMapper = mock(BoardMemberMapper.class);

    private final PostReactionService postReactionService = new PostReactionService(
            postReactionMapper, postMapper, boardMapper, boardMemberMapper);

    @Test
    @DisplayName("getUserReaction: PostReactionMapper.getUserReaction 를 그대로 위임한다")
    void getUserReaction_delegatesToMapper() {
        given(postReactionMapper.getUserReaction(1L, "user@haru.com")).willReturn(null);

        assertNull(postReactionService.getUserReaction(1L, "user@haru.com"));
    }

    @Test
    @DisplayName("applyReaction: 신규 반응이 LIKE 면 insertReaction + like_count 증가")
    void applyReaction_newLikeReaction_insertsAndIncrements() {
        given(postReactionMapper.getUserReaction(1L, "user@haru.com")).willReturn(null);
        given(postReactionMapper.getReactionStatistics(1L)).willReturn(Map.of("LIKE", 1));

        Map<String, Integer> statistics = postReactionService.applyReaction(1L, "user@haru.com", "LIKE");

        verify(postReactionMapper).insertReaction(1L, "user@haru.com", "LIKE");
        verify(postMapper).incrementLikeCount(1L);
        assertEquals(Map.of("LIKE", 1), statistics);
    }

    @Test
    @DisplayName("applyReaction: LIKE 에서 다른 타입으로 바뀌면 like_count 감소")
    void applyReaction_switchAwayFromLike_decrements() {
        PostReaction existing = PostReaction.builder().postId(1L).userEmail("user@haru.com")
                .reactionType("LIKE").build();
        given(postReactionMapper.getUserReaction(1L, "user@haru.com")).willReturn(existing);
        given(postReactionMapper.getReactionStatistics(1L)).willReturn(Map.of());

        postReactionService.applyReaction(1L, "user@haru.com", "HAHA");

        verify(postReactionMapper).updateReactionType(1L, "user@haru.com", "HAHA");
        verify(postMapper).decrementLikeCount(1L);
        verify(postMapper, never()).incrementLikeCount(eq(1L));
    }

    @Test
    @DisplayName("applyReaction: 같은 타입이면 like_count 를 건드리지 않는다")
    void applyReaction_sameType_noCountChange() {
        PostReaction existing = PostReaction.builder().postId(1L).userEmail("user@haru.com")
                .reactionType("LIKE").build();
        given(postReactionMapper.getUserReaction(1L, "user@haru.com")).willReturn(existing);
        given(postReactionMapper.getReactionStatistics(1L)).willReturn(Map.of());

        postReactionService.applyReaction(1L, "user@haru.com", "LIKE");

        verify(postMapper, never()).incrementLikeCount(1L);
        verify(postMapper, never()).decrementLikeCount(1L);
        verify(postReactionMapper, never()).updateReactionType(eq(1L), eq("user@haru.com"), eq("LIKE"));
    }

    @Test
    @DisplayName("deleteReactionAndSync: 삭제 대상이 LIKE 였으면 like_count 감소 후 삭제")
    void deleteReactionAndSync_likeReaction_decrementsThenDeletes() {
        given(postReactionMapper.getReactionStatistics(1L)).willReturn(Map.of());

        Map<String, Integer> statistics = postReactionService.deleteReactionAndSync(1L, "user@haru.com", "LIKE");

        verify(postMapper).decrementLikeCount(1L);
        verify(postReactionMapper).deleteReaction(1L, "user@haru.com");
        assertEquals(Map.of(), statistics);
    }

    @Test
    @DisplayName("deleteReactionAndSync: 삭제 대상이 LIKE 가 아니면 like_count 를 건드리지 않는다")
    void deleteReactionAndSync_nonLikeReaction_doesNotTouchCount() {
        given(postReactionMapper.getReactionStatistics(1L)).willReturn(Map.of());

        postReactionService.deleteReactionAndSync(1L, "user@haru.com", "HAHA");

        verify(postMapper, never()).decrementLikeCount(1L);
        verify(postReactionMapper).deleteReaction(1L, "user@haru.com");
    }

    @Test
    @DisplayName("togglePostLike: 미반응 상태면 좋아요 추가 + 카운트 증가")
    void togglePostLike_addsLike() {
        given(postReactionMapper.hasUserReacted(1L, "user@haru.com")).willReturn(false);
        Post post = Post.builder().likeCount(5).build();
        given(postMapper.getPostById(1L)).willReturn(post);

        Map<String, Object> result = postReactionService.togglePostLike(1L, "user@haru.com");

        verify(postReactionMapper).insertReaction(1L, "user@haru.com", "LIKE");
        verify(postMapper).incrementLikeCount(1L);
        assertEquals(true, result.get("liked"));
        assertEquals(5, result.get("likeCount"));
    }

    @Test
    @DisplayName("togglePostLike: 이미 반응한 상태면 좋아요 취소 + 카운트 감소")
    void togglePostLike_removesLike() {
        given(postReactionMapper.hasUserReacted(1L, "user@haru.com")).willReturn(true);
        Post post = Post.builder().likeCount(4).build();
        given(postMapper.getPostById(1L)).willReturn(post);

        Map<String, Object> result = postReactionService.togglePostLike(1L, "user@haru.com");

        verify(postReactionMapper).deleteReaction(1L, "user@haru.com");
        verify(postMapper).decrementLikeCount(1L);
        assertEquals(false, result.get("liked"));
        assertEquals(4, result.get("likeCount"));
    }
}
