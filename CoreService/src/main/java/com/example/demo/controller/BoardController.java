package com.example.demo.controller;

import com.example.demo.dto.board.*;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.model.board.BoardMember;
import com.example.demo.service.BoardService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/core/boards")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final BoardService boardService;
    private final TokenUtils tokenUtils;

    /**
     * 게시판 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createBoard(
            @RequestHeader("Authorization") String token,
            @RequestBody BoardCreateRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            BoardResponse response = boardService.createBoard(email, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("게시판 생성 중 오류: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "400"));
        }
    }

    /**
     * 게시판 상세 정보 조회
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<?>> getBoardById(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            BoardResponse response = boardService.getBoardById(boardId, email);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("게시판 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시판 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 호스트의 게시판 목록 조회
     */
    @GetMapping("/hosted")
    public ResponseEntity<ApiResponse<?>> getBoardsByHost(
            @RequestHeader("Authorization") String token) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            List<BoardResponse> boards = boardService.getBoardsByHost(email);
            return ResponseEntity.ok(ApiResponse.success(boards));
        } catch (Exception e) {
            log.error("호스트 게시판 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 멤버로 참여 중인 게시판 목록 조회
     */
    @GetMapping("/joined")
    public ResponseEntity<ApiResponse<?>> getBoardsByMember(
            @RequestHeader("Authorization") String token) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            List<BoardResponse> boards = boardService.getBoardsByMember(email);
            return ResponseEntity.ok(ApiResponse.success(boards));
        } catch (Exception e) {
            log.error("멤버 게시판 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 정보 수정
     */
    @PutMapping("/{boardId}")
    public ResponseEntity<ApiResponse<?>> updateBoard(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody BoardCreateRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            BoardResponse response = boardService.updateBoard(email, boardId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("게시판 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시판 수정 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 이미지 업로드
     */
    @PostMapping(value = "/{boardId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadBoardImage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestParam("image") MultipartFile image) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            BoardResponse response = boardService.uploadBoardImage(email, boardId, image);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("이미지 업로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("이미지 업로드 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 멤버 초대
     */
    @PostMapping("/{boardId}/members/invite")
    public ResponseEntity<ApiResponse<?>> inviteMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody MemberInviteRequest request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // 초대 요청의 이메일과 역할을 로그로 확인
            log.info("멤버 초대 요청: boardId={}, inviteEmail={}, role={}", 
                    boardId, request.getEmail(), request.getRole());
            
            if (request.getEmail() == null || request.getEmail().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("초대할 사용자의 이메일이 필요합니다.", "400"));
            }
            
            BoardResponse response = boardService.inviteMember(email, boardId, request.getEmail(), request.getRole());
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("멤버 초대 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("멤버 초대 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 초대 수락
     */
    @PostMapping("/{boardId}/members/accept")
    public ResponseEntity<ApiResponse<?>> acceptInvitation(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            BoardMember member = boardService.acceptInvitation(email, boardId);
            Map<String, Object> response = Map.of(
                "message", "초대를 수락했습니다.",
                "memberId", member.getId(),
                "status", member.getStatus(),
                "role", member.getRole()
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("초대 수락 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("초대 수락 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 초대 거절
     */
    @PostMapping("/{boardId}/members/reject")
    public ResponseEntity<ApiResponse<String>> rejectInvitation(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            boardService.rejectInvitation(email, boardId);
            return ResponseEntity.ok(ApiResponse.success("초대를 거절했습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("초대 거절 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("초대 거절 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 멤버 추방
     */
    @DeleteMapping("/{boardId}/members/{memberId}")
    public ResponseEntity<ApiResponse<String>> kickMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @PathVariable Long memberId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            boardService.kickMember(email, boardId, memberId);
            return ResponseEntity.ok(ApiResponse.success("멤버가 추방되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("멤버 추방 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("멤버 추방 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 삭제
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<ApiResponse<String>> deleteBoard(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            boardService.deleteBoard(email, boardId);
            return ResponseEntity.ok(ApiResponse.success("게시판이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("게시판 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시판 삭제 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 상태 변경
     */
    @PutMapping("/{boardId}/status")
    public ResponseEntity<ApiResponse<?>> updateBoardStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody Map<String, String> request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        String status = request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("상태 값이 필요합니다.", "400"));
        }
        
        try {
            BoardResponse response = boardService.updateBoardStatus(email, boardId, status);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("게시판 상태 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시판 상태 변경 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 호스트 변경
     */
    @PutMapping("/{boardId}/host")
    public ResponseEntity<ApiResponse<?>> changeHost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody Map<String, String> request) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        String newHostEmail = request.get("newHostEmail");
        if (newHostEmail == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("새 호스트 이메일이 필요합니다.", "400"));
        }
        
        try {
            BoardResponse response = boardService.changeHost(email, boardId, newHostEmail);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            log.warn("호스트 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("호스트 변경 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시판 멤버 목록 조회
     */
    @GetMapping("/{boardId}/members")
    public ResponseEntity<ApiResponse<?>> getBoardMembers(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            List<BoardMember> members = boardService.getBoardMembers(email, boardId);
            return ResponseEntity.ok(ApiResponse.success(members));
        } catch (IllegalArgumentException e) {
            log.warn("게시판 멤버 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "400"));
        } catch (Exception e) {
            log.error("게시판 멤버 목록 조회 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }
}
