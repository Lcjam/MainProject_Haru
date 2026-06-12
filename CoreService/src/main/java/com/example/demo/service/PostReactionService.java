package com.example.demo.service;

import com.example.demo.dto.board.PostReactionRequest;
import com.example.demo.dto.board.PostReactionResponse;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.mapper.board.PostReactionMapper;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import com.example.demo.model.board.Post;
import com.example.demo.model.board.PostReaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostReactionService {

    private final PostReactionMapper postReactionMapper;
    private final PostMapper postMapper;
    private final BoardMapper boardMapper;
    private final BoardMemberMapper boardMemberMapper;

    /**
     * 게시글 반응 추가/수정
     */
    @Transactional
    public PostReactionResponse reactToPost(String userEmail, Long postId, PostReactionRequest request) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(userEmail);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(userEmail, board.getId());
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 반응을 추가할 수 있습니다.");
        }
        
        // 반응 유형 검증
        String reactionType = request.getReactionType();
        if (reactionType == null || reactionType.isEmpty()) {
            throw new IllegalArgumentException("반응 유형이 필요합니다.");
        }
        
        // 허용된 반응 유형 검증
        if (!isValidReactionType(reactionType)) {
            throw new IllegalArgumentException("허용되지 않는 반응 유형입니다: " + reactionType);
        }
        
        // 기존 반응 조회
        PostReaction existingReaction = postReactionMapper.getUserReaction(postId, userEmail);
        
        // 반응 처리
        if (existingReaction == null) {
            // 새 반응 추가
            PostReaction reaction = PostReaction.builder()
                    .postId(postId)
                    .userEmail(userEmail)
                    .reactionType(reactionType)
                    .build();
            
            postReactionMapper.addReaction(reaction);
        } else if (existingReaction.getReactionType().equals(reactionType)) {
            // 같은 유형의 반응이 이미 있으면 삭제 (토글)
            postReactionMapper.deleteReaction(postId, userEmail);
            
            // 반응 제거 응답 구성
            return buildReactionResponse(postId, userEmail, null, false);
        } else {
            // 다른 유형의 반응으로 변경
            existingReaction.setReactionType(reactionType);
            postReactionMapper.updateReaction(existingReaction);
        }
        
        // 반응 결과 응답 구성
        return buildReactionResponse(postId, userEmail, reactionType, true);
    }

    /**
     * 게시글 반응 삭제
     */
    @Transactional
    public PostReactionResponse removeReaction(String userEmail, Long postId) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 반응 존재 여부 확인
        PostReaction existingReaction = postReactionMapper.getUserReaction(postId, userEmail);
        if (existingReaction == null) {
            throw new IllegalArgumentException("삭제할 반응이 없습니다.");
        }
        
        // 반응 삭제
        postReactionMapper.deleteReaction(postId, userEmail);
        
        // 반응 결과 응답 구성
        return buildReactionResponse(postId, userEmail, null, false);
    }

    /**
     * 게시글 반응 조회
     */
    public PostReactionResponse getPostReaction(String userEmail, Long postId) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 사용자의 반응 조회
        PostReaction userReaction = postReactionMapper.getUserReaction(postId, userEmail);
        String reactionType = userReaction != null ? userReaction.getReactionType() : null;
        boolean isReacted = userReaction != null;
        
        // 반응 결과 응답 구성
        return buildReactionResponse(postId, userEmail, reactionType, isReacted);
    }

    /**
     * 게시글 반응 목록 조회
     */
    public List<PostReactionResponse> getPostReactions(String userEmail, Long postId) {
        // 게시글 존재 여부 확인
        Post post = postMapper.getPostById(postId);
        if (post == null) {
            throw new IllegalArgumentException("게시글을 찾을 수 없습니다.");
        }
        
        // 사용자가 게시판의 멤버인지 확인
        Board board = boardMapper.findBoardById(post.getBoardId());
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        boolean isMember = false;
        boolean isHost = board.getHostEmail().equals(userEmail);
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(userEmail, board.getId());
            if (member != null && "ACTIVE".equals(member.getStatus())) {
                isMember = true;
            }
        }
        
        if (!isHost && !isMember) {
            throw new IllegalArgumentException("게시판의 멤버만 반응 목록을 볼 수 있습니다.");
        }
        
        // 게시글의 모든 반응 조회
        List<PostReaction> reactions = postReactionMapper.getPostReactions(postId);
        
        // 반응 타입별 개수 조회
        Map<String, Integer> reactionCounts = getReactionCountsMap(postId);
        
        // 반응 결과 응답 목록 구성
        return reactions.stream()
                .map(reaction -> PostReactionResponse.builder()
                        .postId(reaction.getPostId())
                        .userEmail(reaction.getUserEmail())
                        .reactionType(reaction.getReactionType())
                        .createdAt(reaction.getCreatedAt())
                        .isReacted(reaction.getUserEmail().equals(userEmail))
                        .reactionCounts(reactionCounts)
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 반응 응답 구성 메서드
     */
    private PostReactionResponse buildReactionResponse(Long postId, String userEmail, String reactionType, boolean isReacted) {
        // 반응 타입별 개수 조회
        Map<String, Integer> reactionCounts = getReactionCountsMap(postId);
        
        // 응답 객체 구성
        return PostReactionResponse.builder()
                .postId(postId)
                .userEmail(userEmail)
                .reactionType(reactionType)
                .isReacted(isReacted)
                .reactionCounts(reactionCounts)
                .build();
    }

    /**
     * 반응 타입별 개수 맵 조회
     */
    private Map<String, Integer> getReactionCountsMap(Long postId) {
        List<Map<String, Object>> counts = postReactionMapper.getReactionCounts(postId);
        Map<String, Integer> reactionCounts = new HashMap<>();
        
        for (Map<String, Object> count : counts) {
            String type = (String) count.get("reactionType");
            Integer countValue = ((Number) count.get("count")).intValue();
            reactionCounts.put(type, countValue);
        }
        
        return reactionCounts;
    }

    /**
     * 반응 유형 유효성 검증
     */
    private boolean isValidReactionType(String reactionType) {
        return reactionType.matches("^[A-Z_]{1,20}$");
    }
}
