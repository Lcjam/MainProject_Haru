package com.example.demo.controller.board;

import com.example.demo.dto.board.PostReactionRequest;
import com.example.demo.dto.board.PostReactionResponse;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.mapper.board.PostMapper;
import com.example.demo.mapper.board.PostReactionMapper;
import com.example.demo.model.board.PostReaction;
import com.example.demo.service.PostReactionService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core/boards")
@RequiredArgsConstructor
@Slf4j
public class PostReactionController {

    private final PostReactionService postReactionService;
    private final TokenUtils tokenUtils;
    private final PostReactionMapper postReactionMapper;
    private final PostMapper postMapper;

    /**
     * 게시글 반응 추가/변경
     */
    @PostMapping("/posts/{postId}/reactions")
    public ResponseEntity<ApiResponse<?>> addReaction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @RequestBody Map<String, String> request) {

        String email = tokenUtils.getEmailFromAuthHeader(token);
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }

        String reactionType = request.get("reactionType");
        if (reactionType == null || reactionType.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("반응 타입은 필수입니다.", "400"));
        }

        try {
            // 현재 사용자의 반응 확인
            PostReaction existingReaction = postReactionMapper.getUserReaction(postId, email);
            
            // 반응 타입이 'LIKE'인 경우 like_count 처리
            boolean isLikeReaction = "LIKE".equals(reactionType);
            boolean isLikeExisting = existingReaction != null && "LIKE".equals(existingReaction.getReactionType());
            
            if (existingReaction == null) {
                // 새 반응 추가
                postReactionMapper.insertReaction(postId, email, reactionType);
                
                // LIKE 타입이면 like_count 증가
                if (isLikeReaction) {
                    postMapper.incrementLikeCount(postId);
                }
            } else {
                // 기존 반응 수정 (타입이 다른 경우)
                if (!reactionType.equals(existingReaction.getReactionType())) {
                    postReactionMapper.updateReactionType(postId, email, reactionType);
                    
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
            Map<String, Integer> statistics = postReactionMapper.getReactionStatistics(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("postId", postId);
            response.put("userEmail", email);
            response.put("reactionType", reactionType);
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("반응 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("반응 처리 중 오류가 발생했습니다: " + e.getMessage(), "500"));
        }
    }

    /**
     * 게시글 반응 삭제
     */
    @DeleteMapping("/posts/{postId}/reactions")
    public ResponseEntity<ApiResponse<?>> deleteReaction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {

        String email = tokenUtils.getEmailFromAuthHeader(token);
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }

        try {
            PostReaction existingReaction = postReactionMapper.getUserReaction(postId, email);
            if (existingReaction == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("삭제할 반응이 없습니다.", "404"));
            }
            
            // 'LIKE' 반응이면 like_count 감소
            if ("LIKE".equals(existingReaction.getReactionType())) {
                postMapper.decrementLikeCount(postId);
            }

            // 반응 삭제
            postReactionMapper.deleteReaction(postId, email);
            
            // 게시글의 현재 반응 통계 조회
            Map<String, Integer> statistics = postReactionMapper.getReactionStatistics(postId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "반응이 삭제되었습니다.");
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("반응 삭제 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("반응 삭제 중 오류가 발생했습니다: " + e.getMessage(), "500"));
        }
    }

    /**
     * 게시글 반응 조회 (사용자의 반응 및 전체 반응 통계)
     */
    @GetMapping("/posts/{postId}/reactions")
    public ResponseEntity<ApiResponse<?>> getPostReaction(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            PostReactionResponse response = postReactionService.getPostReaction(email, postId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("게시글 반응 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시글 반응 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시글 반응 목록 조회
     */
    @GetMapping("/posts/{postId}/reactions/list")
    public ResponseEntity<ApiResponse<?>> getPostReactions(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            List<PostReactionResponse> responses = postReactionService.getPostReactions(email, postId);
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (IllegalArgumentException e) {
            log.warn("게시글 반응 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시글 반응 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<?>> toggleLike(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {

        String email = tokenUtils.getEmailFromAuthHeader(token);
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }

        try {
            // 이미 좋아요를 눌렀는지 확인
            boolean alreadyLiked = postReactionMapper.hasUserReacted(postId, email);
            Map<String, Object> response = new HashMap<>();

            if (alreadyLiked) {
                // 좋아요 취소: 반응 삭제 및 카운트 감소
                postReactionMapper.deleteReaction(postId, email);
                postMapper.decrementLikeCount(postId);
                
                response.put("liked", false);
                response.put("message", "좋아요가 취소되었습니다.");
            } else {
                // 좋아요 추가: 반응 추가 및 카운트 증가
                postReactionMapper.insertReaction(postId, email, "LIKE");
                postMapper.incrementLikeCount(postId);
                
                response.put("liked", true);
                response.put("message", "좋아요가 추가되었습니다.");
            }
            
            // 업데이트된 좋아요 수를 응답에 포함
            Integer likeCount = postMapper.getPostById(postId).getLikeCount();
            response.put("likeCount", likeCount);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("좋아요 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("좋아요 처리 중 오류가 발생했습니다: " + e.getMessage(), "500"));
        }
    }
}
