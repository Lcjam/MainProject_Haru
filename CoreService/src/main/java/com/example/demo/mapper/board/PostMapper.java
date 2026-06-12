package com.example.demo.mapper.board;

import com.example.demo.model.board.Post;
import com.example.demo.model.board.PostImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PostMapper {
    // 게시글 생성
    void createPost(Post post);
    
    // 게시글 이미지 추가
    void addPostImage(PostImage postImage);
    
    // 게시글 수정
    void updatePost(Post post);
    
    // 게시글 삭제 (소프트 삭제)
    void deletePost(@Param("id") Long id, @Param("email") String email);
    
    // 게시글 조회 (ID로)
    Post getPostById(@Param("id") Long id);
    
    // 게시글 조회수 증가
    void increaseViewCount(@Param("id") Long id);
    
    // 게시판 내 게시글 목록 조회
    List<Post> getPostsByBoardId(@Param("boardId") Long boardId);
    
    // 게시판 내 게시글 개수 조회
    int countPostsByBoardId(@Param("boardId") Long boardId);
    
    // 사용자가 작성한 게시글 목록 조회
    List<Post> getPostsByAuthorEmail(@Param("email") String email);
    
    // 게시글의 이미지 목록 조회
    List<PostImage> getPostImagesByPostId(@Param("postId") Long postId);
    
    // 게시글 이미지 삭제
    void deletePostImages(@Param("postId") Long postId);
    
    // 게시글 검색
    List<Post> searchPosts(@Param("boardId") Long boardId, @Param("keyword") String keyword);
    
    // 고급 검색 및 필터링
    List<Post> findPostsWithFilters(
            @Param("boardId") Long boardId,
            @Param("keyword") String keyword,
            @Param("searchField") String searchField,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("sortBy") String sortBy,
            @Param("sortDirection") String sortDirection,
            @Param("offset") int offset,
            @Param("limit") int limit);
    
    // 필터 적용된 게시글 총 개수 조회
    int countPostsWithFilters(
            @Param("boardId") Long boardId,
            @Param("keyword") String keyword,
            @Param("searchField") String searchField,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 게시글 좋아요 수 증가
    void incrementLikeCount(@Param("postId") Long postId);

    // 게시글 좋아요 수 감소
    void decrementLikeCount(@Param("postId") Long postId);
}
