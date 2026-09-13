package com.example.demo.controller.board;

import com.example.demo.dto.board.PostReactionRequest;
import com.example.demo.dto.board.PostReactionResponse;
import com.example.demo.dto.response.ApiResponse;
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
            // 반응 추가/변경 + like_count 동기화
            Map<String, Integer> statistics = postReactionService.applyReaction(postId, email, reactionType);

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
            PostReaction existingReaction = postReactionService.getUserReaction(postId, email);
            if (existingReaction == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("삭제할 반응이 없습니다.", "404"));
            }

            // 반응 삭제 + like_count 동기화
            Map<String, Integer> statistics = postReactionService.deleteReactionAndSync(
                    postId, email, existingReaction.getReactionType());

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
            // 좋아요 토글 + like_count 동기화
            Map<String, Object> response = postReactionService.togglePostLike(postId, email);

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("좋아요 처리 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("좋아요 처리 중 오류가 발생했습니다: " + e.getMessage(), "500"));
        }
    }
}
