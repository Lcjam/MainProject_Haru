package com.example.demo.service;

import com.example.demo.dto.board.PostCreateRequest;
import com.example.demo.dto.board.PostUpdateRequest;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.Post;
import com.example.demo.model.board.PostImage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PostResponseFlowTest {
    private final PostMapper posts = mock(PostMapper.class);
    private final BoardMapper boards = mock(BoardMapper.class);
    private final BoardMemberMapper members = mock(BoardMemberMapper.class);
    private final PostService service = new PostService(posts, boards, members, mock(FileStorageService.class));

    @Test
    void createEditListAndDetailKeepImagesAuthorFlagsAndViewCountSemantics() {
        var post = Post.builder().id(10L).boardId(1L).authorEmail("host@example.test")
                .title("제목").content("내용").viewCount(5).likeCount(2).commentCount(3).build();
        when(boards.findBoardById(1L)).thenReturn(Board.builder().id(1L).hostEmail(post.getAuthorEmail()).build());
        doAnswer(call -> { ((Post) call.getArgument(0)).setId(10L); return null; }).when(posts).createPost(any());
        when(posts.getPostById(10L)).thenReturn(post);
        when(posts.getPostImagesByPostId(10L)).thenReturn(List.of(PostImage.builder().imageUrl("/photo.png").build()));
        when(posts.getPostsByBoardId(1L)).thenReturn(List.of(post));
        var created = service.createPost(post.getAuthorEmail(), PostCreateRequest.builder()
                .boardId(1L).title("제목").content("내용").imageUrls(List.of("/photo.png")).build());
        var edited = service.updatePost(post.getAuthorEmail(), 10L, PostUpdateRequest.builder()
                .title("제목").content("내용").imageUrls(List.of("/photo.png")).build());
        var listed = service.getPostsByBoardId(post.getAuthorEmail(), 1L).get(0);
        assertEquals(created, edited);
        assertEquals(created, listed);
        assertEquals(List.of("/photo.png"), listed.getImageUrls());
        assertTrue(listed.isAuthor());
        assertEquals(5, listed.getViewCount());
        var detail = service.getPostById(post.getAuthorEmail(), 10L);
        assertEquals(6, detail.getViewCount());
        detail.setViewCount(5);
        assertEquals(listed, detail);
        verify(posts).increaseViewCount(10L);
        assertThrows(IllegalArgumentException.class, () -> service.getPostById("outsider@example.test", 10L));
        verify(posts, times(1)).increaseViewCount(10L);
    }
}
