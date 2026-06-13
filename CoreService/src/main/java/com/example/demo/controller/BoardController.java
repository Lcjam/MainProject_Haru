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

/**
 * 게시판(보드) CRUD/멤버 관리.
 * <p>인증 가드(401)와 예외 변환(IllegalArgumentException→400, 그 외→500)은
 * {@code GlobalExceptionHandler} 가 동일한 {@link ApiResponse} 형태로 처리하므로
 * 각 메서드는 토큰→이메일 확인({@link TokenUtils#requireEmail})과 서비스 위임만 담당한다.
 */
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

        String email = tokenUtils.requireEmail(token);
        BoardResponse response = boardService.createBoard(email, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시판 상세 정보 조회
     */
    @GetMapping("/{boardId}")
    public ResponseEntity<ApiResponse<?>> getBoardById(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {

        String email = tokenUtils.requireEmail(token);
        BoardResponse response = boardService.getBoardById(boardId, email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 호스트의 게시판 목록 조회
     */
    @GetMapping("/hosted")
    public ResponseEntity<ApiResponse<?>> getBoardsByHost(
            @RequestHeader("Authorization") String token) {

        String email = tokenUtils.requireEmail(token);
        List<BoardResponse> boards = boardService.getBoardsByHost(email);
        return ResponseEntity.ok(ApiResponse.success(boards));
    }

    /**
     * 멤버로 참여 중인 게시판 목록 조회
     */
    @GetMapping("/joined")
    public ResponseEntity<ApiResponse<?>> getBoardsByMember(
            @RequestHeader("Authorization") String token) {

        String email = tokenUtils.requireEmail(token);
        List<BoardResponse> boards = boardService.getBoardsByMember(email);
        return ResponseEntity.ok(ApiResponse.success(boards));
    }

    /**
     * 게시판 정보 수정
     */
    @PutMapping("/{boardId}")
    public ResponseEntity<ApiResponse<?>> updateBoard(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody BoardCreateRequest request) {

        String email = tokenUtils.requireEmail(token);
        BoardResponse response = boardService.updateBoard(email, boardId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시판 이미지 업로드
     */
    @PostMapping(value = "/{boardId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadBoardImage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestParam("image") MultipartFile image) {

        String email = tokenUtils.requireEmail(token);
        BoardResponse response = boardService.uploadBoardImage(email, boardId, image);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시판 멤버 초대
     */
    @PostMapping("/{boardId}/members/invite")
    public ResponseEntity<ApiResponse<?>> inviteMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody MemberInviteRequest request) {

        String email = tokenUtils.requireEmail(token);

        log.info("멤버 초대 요청: boardId={}, inviteEmail={}, role={}",
                boardId, request.getEmail(), request.getRole());

        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("초대할 사용자의 이메일이 필요합니다.", "400"));
        }

        BoardResponse response = boardService.inviteMember(email, boardId, request.getEmail(), request.getRole());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 초대 수락
     */
    @PostMapping("/{boardId}/members/accept")
    public ResponseEntity<ApiResponse<?>> acceptInvitation(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {

        String email = tokenUtils.requireEmail(token);
        BoardMember member = boardService.acceptInvitation(email, boardId);
        Map<String, Object> response = Map.of(
            "message", "초대를 수락했습니다.",
            "memberId", member.getId(),
            "status", member.getStatus(),
            "role", member.getRole()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 초대 거절
     */
    @PostMapping("/{boardId}/members/reject")
    public ResponseEntity<ApiResponse<String>> rejectInvitation(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {

        String email = tokenUtils.requireEmail(token);
        boardService.rejectInvitation(email, boardId);
        return ResponseEntity.ok(ApiResponse.success("초대를 거절했습니다."));
    }

    /**
     * 멤버 추방
     */
    @DeleteMapping("/{boardId}/members/{memberId}")
    public ResponseEntity<ApiResponse<String>> kickMember(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @PathVariable Long memberId) {

        String email = tokenUtils.requireEmail(token);
        boardService.kickMember(email, boardId, memberId);
        return ResponseEntity.ok(ApiResponse.success("멤버가 추방되었습니다."));
    }

    /**
     * 게시판 삭제
     */
    @DeleteMapping("/{boardId}")
    public ResponseEntity<ApiResponse<String>> deleteBoard(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {

        String email = tokenUtils.requireEmail(token);
        boardService.deleteBoard(email, boardId);
        return ResponseEntity.ok(ApiResponse.success("게시판이 삭제되었습니다."));
    }

    /**
     * 게시판 상태 변경
     */
    @PutMapping("/{boardId}/status")
    public ResponseEntity<ApiResponse<?>> updateBoardStatus(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody Map<String, String> request) {

        String email = tokenUtils.requireEmail(token);

        String status = request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("상태 값이 필요합니다.", "400"));
        }

        BoardResponse response = boardService.updateBoardStatus(email, boardId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시판 호스트 변경
     */
    @PutMapping("/{boardId}/host")
    public ResponseEntity<ApiResponse<?>> changeHost(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestBody Map<String, String> request) {

        String email = tokenUtils.requireEmail(token);

        String newHostEmail = request.get("newHostEmail");
        if (newHostEmail == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("새 호스트 이메일이 필요합니다.", "400"));
        }

        BoardResponse response = boardService.changeHost(email, boardId, newHostEmail);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 게시판 멤버 목록 조회
     */
    @GetMapping("/{boardId}/members")
    public ResponseEntity<ApiResponse<?>> getBoardMembers(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId) {

        String email = tokenUtils.requireEmail(token);
        List<BoardMember> members = boardService.getBoardMembers(email, boardId);
        return ResponseEntity.ok(ApiResponse.success(members));
    }
}
