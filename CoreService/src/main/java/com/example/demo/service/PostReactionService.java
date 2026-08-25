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
     * 게시글의 특정 사용자 반응 조회 (PostReactionController.deleteReaction 의 404 판단용 — 판단은 컨트롤러에 남긴다)
     * PostReactionController.addReaction / deleteReaction 이 직접 쓰던 PostReactionMapper.getUserReaction 을 그대로 옮긴 통과 메서드다.
     */
    public PostReaction getUserReaction(Long postId, String userEmail) {
        return postReactionMapper.getUserReaction(postId, userEmail);
    }

    /**
     * PostReactionController.addReaction(POST /posts/{postId}/reactions) 의 반응 추가/변경 + like_count 동기화.
     * 주의: reactToPost(:37) 와 이름은 비슷하지만 별개 메서드다 — reactToPost 는 호출부 0의 죽은 코드이고
     * 게시판 멤버십 검증까지 포함하지만, 컨트롤러의 현행 동작은 멤버십 검증이 없다. "순수 이동" 원칙에 따라
     * reactToPost 를 재사용하지 않고 컨트롤러의 현행 로직을 그대로 옮겼다.
     */
    public Map<String, Integer> applyReaction(Long postId, String userEmail, String reactionType) {
        // 현재 사용자의 반응 확인
        PostReaction existingReaction = postReactionMapper.getUserReaction(postId, userEmail);

        // 반응 타입이 'LIKE'인 경우 like_count 처리
        boolean isLikeReaction = "LIKE".equals(reactionType);
        boolean isLikeExisting = existingReaction != null && "LIKE".equals(existingReaction.getReactionType());

        if (existingReaction == null) {
            // 새 반응 추가
            postReactionMapper.insertReaction(postId, userEmail, reactionType);

            // LIKE 타입이면 like_count 증가
            if (isLikeReaction) {
                postMapper.incrementLikeCount(postId);
            }
        } else {
            // 기존 반응 수정 (타입이 다른 경우)
            if (!reactionType.equals(existingReaction.getReactionType())) {
                postReactionMapper.updateReactionType(postId, userEmail, reactionType);

                // like_count 처리 (이전 반응과 현재 반응의 LIKE 상태에 따라)
                if (isLikeExisting && !isLikeReaction) {
                    // LIKE -> 다른 타입으로 변경: like_count 감소
                    postMapper.decrementLikeCount(postId);
                } else if (!isLikeExisting && isLikeReaction) {
                    // 다른 타입 -> LIKE로 변경: like_count 증가
                    postMapper.incrementLikeCount(postId);
                }
            }
        }

        // 게시글의 현재 반응 통계 조회
        return postReactionMapper.getReactionStatistics(postId);
    }

    /**
     * PostReactionController.deleteReaction(DELETE /posts/{postId}/reactions) 의 반응 삭제 + like_count 동기화.
     * 삭제할 반응이 있는지(404 판단)는 컨트롤러가 getUserReaction 으로 먼저 확인하고, 그 결과의 reactionType 을
     * 여기로 전달한다. removeReaction(:108) 은 호출부 0의 죽은 코드라 재사용하지 않고 컨트롤러의 현행 로직을
     * 그대로 옮겼다.
     */
    public Map<String, Integer> deleteReactionAndSync(Long postId, String userEmail, String currentReactionType) {
        // 'LIKE' 반응이면 like_count 감소
        if ("LIKE".equals(currentReactionType)) {
            postMapper.decrementLikeCount(postId);
        }

        // 반응 삭제
        postReactionMapper.deleteReaction(postId, userEmail);

        // 게시글의 현재 반응 통계 조회
        return postReactionMapper.getReactionStatistics(postId);
    }

    /**
     * PostReactionController.toggleLike(POST /{postId}/like) 의 좋아요 토글 + like_count 동기화.
     * 응답 맵의 키(liked/message/likeCount)는 컨트롤러의 현행 응답 구성과 동일하게 유지한다.
     */
    public Map<String, Object> togglePostLike(Long postId, String userEmail) {
        // 이미 좋아요를 눌렀는지 확인
        boolean alreadyLiked = postReactionMapper.hasUserReacted(postId, userEmail);
        Map<String, Object> response = new HashMap<>();

        if (alreadyLiked) {
            // 좋아요 취소: 반응 삭제 및 카운트 감소
            postReactionMapper.deleteReaction(postId, userEmail);
            postMapper.decrementLikeCount(postId);

            response.put("liked", false);
            response.put("message", "좋아요가 취소되었습니다.");
        } else {
            // 좋아요 추가: 반응 추가 및 카운트 증가
            postReactionMapper.insertReaction(postId, userEmail, "LIKE");
            postMapper.incrementLikeCount(postId);

            response.put("liked", true);
            response.put("message", "좋아요가 추가되었습니다.");
        }

        // 업데이트된 좋아요 수를 응답에 포함
        Integer likeCount = postMapper.getPostById(postId).getLikeCount();
        response.put("likeCount", likeCount);

        return response;
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
