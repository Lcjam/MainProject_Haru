package com.example.demo.controller.board;

import com.example.demo.dto.board.CommentCreateRequest;
import com.example.demo.dto.board.CommentResponse;
import com.example.demo.dto.board.CommentUpdateRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.service.CommentService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/core/boards")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;
    private final TokenUtils tokenUtils;

    /**
     * 댓글 생성
     */
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<?>> createComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @RequestBody CommentCreateRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // postId 설정
            request.setPostId(postId);
            
            CommentResponse response = commentService.createComment(email, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("댓글 작성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("댓글 작성 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 대댓글 생성
     */
    @PostMapping("/posts/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<?>> createReply(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentCreateRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // postId와 parentId 설정
            request.setPostId(postId);
            request.setParentId(commentId);
            
            CommentResponse response = commentService.createComment(email, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("대댓글 작성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("대댓글 작성 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<?>> updateComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            CommentResponse response = commentService.updateComment(email, commentId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("댓글 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("댓글 수정 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/posts/{postId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            commentService.deleteComment(email, commentId);
            return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("댓글 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("댓글 삭제 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시글의 댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<?>> getCommentsByPostId(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            List<CommentResponse> comments = commentService.getCommentsByPostId(email, postId);
            return ResponseEntity.ok(ApiResponse.success(comments));
        } catch (IllegalArgumentException e) {
            log.warn("댓글 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("댓글 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 대댓글 목록 조회
     */
    @GetMapping("/posts/{postId}/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<?>> getRepliesByParentId(
            @RequestHeader("Authorization") String token,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            List<CommentResponse> replies = commentService.getRepliesByParentId(email, commentId);
            return ResponseEntity.ok(ApiResponse.success(replies));
        } catch (IllegalArgumentException e) {
            log.warn("대댓글 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("대댓글 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }
}
