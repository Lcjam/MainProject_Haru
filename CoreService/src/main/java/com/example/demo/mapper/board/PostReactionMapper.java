package com.example.demo.mapper.board;

import com.example.demo.model.board.PostReaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostReactionMapper {
    // 게시글 반응 추가
    void addReaction(PostReaction reaction);
    
    // 게시글 반응 수정
    void updateReaction(PostReaction reaction);
    
    // 게시글 반응 삭제
    void deleteReaction(@Param("postId") Long postId, @Param("userEmail") String userEmail);
    
    // 게시글의 특정 사용자 반응 조회
    PostReaction getUserReaction(@Param("postId") Long postId, @Param("userEmail") String userEmail);
    
    // 게시글의 모든 반응 조회
    List<PostReaction> getPostReactions(@Param("postId") Long postId);
    
    // 게시글의 반응 타입별 개수 조회
    List<Map<String, Object>> getReactionCounts(@Param("postId") Long postId);
    
    // 사용자가 작성한 반응 목록 조회
    List<PostReaction> getUserReactions(@Param("userEmail") String userEmail);
    
    // 특정 게시글의 특정 유형 반응 개수 조회
    int countReactionsByType(@Param("postId") Long postId, @Param("reactionType") String reactionType);
    
    // 특정 게시글의 전체 반응 개수 조회
    int countAllReactions(@Param("postId") Long postId);
    
    /**
     * 사용자가 게시글에 이미 반응했는지 확인
     */
    boolean hasUserReacted(@Param("postId") Long postId, @Param("userEmail") String userEmail);
    
    /**
     * 새로운 반응 추가
     */
    void insertReaction(@Param("postId") Long postId, @Param("userEmail") String userEmail, @Param("reactionType") String reactionType);
    
    /**
     * 게시글의 반응 목록 조회
     */
    List<PostReaction> getReactionsByPostId(Long postId);
    
    /**
     * 게시글의 반응 타입별 통계 조회
     */
    Map<String, Integer> getReactionStatistics(@Param("postId") Long postId);
    
    /**
     * 반응 타입 변경
     */
    void updateReactionType(@Param("postId") Long postId, @Param("userEmail") String userEmail, @Param("reactionType") String reactionType);
}
