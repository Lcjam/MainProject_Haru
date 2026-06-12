package com.example.demo.mapper.board;

import com.example.demo.model.board.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {
    // 댓글 생성
    void createComment(Comment comment);
    
    // 댓글 수정
    void updateComment(Comment comment);
    
    // 댓글 삭제 (소프트 삭제)
    void deleteComment(@Param("id") Long id, @Param("email") String email);
    
    // 댓글 ID로 조회
    Comment getCommentById(Long id);
    
    // 게시글의 모든 댓글 조회 (계층 구조, 최상위 댓글만)
    List<Comment> getCommentsByPostId(@Param("postId") Long postId);
    
    // 게시글의 모든 댓글 수 조회
    int countCommentsByPostId(@Param("postId") Long postId);
    
    // 특정 댓글의 대댓글 조회
    List<Comment> getRepliesByParentId(@Param("parentId") Long parentId);
    
    // 특정 댓글의 대댓글 수 조회
    int countRepliesByParentId(@Param("parentId") Long parentId);
    
    // 대댓글 수 업데이트
    void updateReplyCount(@Param("id") Long id, @Param("count") int count);
    
    // 게시글의 댓글 수 업데이트
    void updatePostCommentCount(@Param("postId") Long postId, @Param("count") int count);
    
    // 사용자가 작성한 댓글 조회
    List<Comment> getCommentsByAuthorEmail(@Param("email") String email);
}
