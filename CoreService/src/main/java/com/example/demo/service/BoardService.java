package com.example.demo.service;

import com.example.demo.dto.board.*;
import com.example.demo.mapper.UserMapper;
import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.model.User;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardService {

    private final BoardMapper boardMapper;
    private final BoardMemberMapper boardMemberMapper;
    private final UserMapper userMapper;
    private final TokenUtils tokenUtils;
    private final FileStorageService fileStorageService;

    /**
     * 게시판 생성
     */
    @Transactional
    public BoardResponse createBoard(String userEmail, BoardCreateRequest request) {
        // 사용자 존재 여부 확인
        User user = userMapper.findByEmail(userEmail);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        // 게시판 객체 생성
        Board board = Board.builder()
                .name(request.getName())
                .description(request.getDescription())
                .hostEmail(userEmail)
                .status("ACTIVE")
                .build();

        // 게시판 저장
        boardMapper.createBoard(board);

        // 호스트를 멤버로 추가
        BoardMember hostMember = BoardMember.builder()
                .boardId(board.getId())
                .userEmail(userEmail)
                .role("HOST")
                .status("ACTIVE")
                .invitedBy(userEmail)
                .build();

        boardMemberMapper.addMember(hostMember);

        // 응답 객체 생성
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .imagePath(board.getImagePath())
                .hostEmail(board.getHostEmail())
                .hostName(user.getNickname())
                .memberCount(1) // 처음에는 호스트만 존재
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .status(board.getStatus())
                .build();
    }

    /**
     * 게시판 상세 정보 조회
     */
    public BoardResponse getBoardById(Long boardId, String userEmail) {
        // 게시판 정보 조회
        Board board = boardMapper.findBoardById(boardId);
        if (board == null || "DELETED".equals(board.getStatus())) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 멤버십 확인
        BoardMember membership = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, userEmail);
        if (membership == null || !"ACTIVE".equals(membership.getStatus())) {
            throw new IllegalArgumentException("해당 게시판에 접근 권한이 없습니다.");
        }

        return convertToResponse(board);
    }

    /**
     * 호스트의 게시판 목록 조회
     */
    public List<BoardResponse> getBoardsByHost(String hostEmail) {
        List<Board> boards = boardMapper.findBoardsByHostEmail(hostEmail);

        return boards.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 멤버인 게시판 목록 조회
     */
    public List<BoardResponse> getBoardsByMember(String memberEmail) {
        List<Board> boards = boardMapper.findBoardsByMemberEmail(memberEmail);

        return boards.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 게시판 정보 수정
     */
    @Transactional
    public BoardResponse updateBoard(String userEmail, Long boardId, BoardCreateRequest request) {
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 호스트만 수정 가능
        if (!board.getHostEmail().equals(userEmail)) {
            throw new IllegalArgumentException("게시판 수정 권한이 없습니다.");
        }

        // 게시판 정보 업데이트
        board.setName(request.getName());
        board.setDescription(request.getDescription());
        boardMapper.updateBoard(board);

        // 업데이트된 정보 조회
        Board updatedBoard = boardMapper.findBoardById(boardId);
        return convertToResponse(updatedBoard);
    }

    /**
     * 게시판 이미지 업로드
     */
    @Transactional
    public BoardResponse uploadBoardImage(String userEmail, Long boardId, MultipartFile image) {
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 호스트만 이미지 업로드 가능
        if (!board.getHostEmail().equals(userEmail)) {
            throw new IllegalArgumentException("게시판 수정 권한이 없습니다.");
        }

        try {
            // 기존 이미지가 있으면 삭제
            if (board.getImagePath() != null) {
                // 이미지 삭제 로직 (필요 시 구현)
            }

            // 새 이미지 저장
            String imagePath = "/board-images/" + boardId + "/" + System.currentTimeMillis() + "_" + image.getOriginalFilename();
            fileStorageService.storeFile(image, "board-images/" + boardId, image.getOriginalFilename());

            // 이미지 경로 업데이트
            boardMapper.updateBoardImage(boardId, imagePath);

            // 업데이트된 정보 조회
            Board updatedBoard = boardMapper.findBoardById(boardId);
            return convertToResponse(updatedBoard);
        } catch (Exception e) {
            log.error("게시판 이미지 업로드 중 오류: {}", e.getMessage());
            throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 멤버 초대
     */
    @Transactional
    public BoardResponse inviteMember(String hostEmail, Long boardId, String inviteEmail, String role) {
        // 초대할 이메일 유효성 검증
        log.info("inviteMember 메서드: boardId={}, hostEmail={}, inviteEmail={}, role={}", 
                boardId, hostEmail, inviteEmail, role);
        
        // 초대 대상 사용자가 존재하는지 확인
        User inviteUser = userMapper.findByEmail(inviteEmail);
        if (inviteUser == null) {
            throw new IllegalArgumentException("초대 대상 사용자를 찾을 수 없습니다.");
        }
        
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 초대 권한 확인 (호스트 또는 관리자)
        BoardMember inviterMembership = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, hostEmail);
        if (inviterMembership == null || 
            (!inviterMembership.getRole().equals("HOST") && !inviterMembership.getRole().equals("ADMIN"))) {
            throw new IllegalArgumentException("멤버 초대 권한이 없습니다.");
        }

        // 초대 대상 사용자 확인
        User invitee = userMapper.findByEmail(inviteEmail);
        if (invitee == null) {
            throw new IllegalArgumentException("초대 대상 사용자를 찾을 수 없습니다.");
        }

        // 이미 멤버인지 확인
        BoardMember existingMember = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, inviteEmail);
        if (existingMember != null) {
            if (existingMember.getStatus().equals("ACTIVE")) {
                throw new IllegalArgumentException("이미 게시판 멤버입니다.");
            } else if (existingMember.getStatus().equals("PENDING")) {
                throw new IllegalArgumentException("이미 초대가 진행 중입니다.");
            } else {
                // BANNED 상태인 경우 상태 업데이트
                boardMemberMapper.updateMemberStatus(existingMember.getId(), "PENDING");
                return convertToResponse(board);
            }
        }

        // 새 멤버 추가
        BoardMember newMember = BoardMember.builder()
                .boardId(boardId)
                .userEmail(inviteEmail)
                .role(role)
                .status("PENDING") // 초대 상태로 시작
                .invitedBy(hostEmail)
                .build();

        boardMemberMapper.addMember(newMember);

        // TODO: 초대 알림 발송 로직 추가

        return convertToResponse(board);
    }

    /**
     * 초대 수락
     */
    @Transactional
    public BoardMember acceptInvitation(String userEmail, Long boardId) {
        BoardMember membership = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, userEmail);
        if (membership == null) {
            throw new IllegalArgumentException("초대 정보를 찾을 수 없습니다.");
        }

        if (!membership.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("유효한 초대가 아닙니다.");
        }

        // 초대 수락
        boardMemberMapper.updateMemberStatus(membership.getId(), "ACTIVE");
        
        return boardMemberMapper.findMemberById(membership.getId());
    }

    /**
     * 초대 거절
     */
    @Transactional
    public void rejectInvitation(String userEmail, Long boardId) {
        BoardMember membership = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, userEmail);
        if (membership == null) {
            throw new IllegalArgumentException("초대 정보를 찾을 수 없습니다.");
        }

        if (!membership.getStatus().equals("PENDING")) {
            throw new IllegalArgumentException("유효한 초대가 아닙니다.");
        }

        // 초대 거절 (멤버십 삭제)
        boardMemberMapper.deleteMember(membership.getId());
    }

    /**
     * 멤버 추방
     */
    @Transactional
    public void kickMember(String hostEmail, Long boardId, Long memberId) {
        // 게시판 정보 조회
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 호스트 권한 확인
        if (!board.getHostEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("멤버 추방 권한이 없습니다.");
        }

        // 멤버 정보 조회
        BoardMember member = boardMemberMapper.findMemberById(memberId);
        if (member == null || !member.getBoardId().equals(boardId)) {
            throw new IllegalArgumentException("해당 멤버를 찾을 수 없습니다.");
        }

        // 호스트는 추방할 수 없음
        if (member.getRole().equals("HOST")) {
            throw new IllegalArgumentException("호스트는 추방할 수 없습니다.");
        }

        // 멤버 추방 (상태를 BANNED로 변경)
        boardMemberMapper.updateMemberStatus(memberId, "BANNED");
    }

    /**
     * 게시판 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteBoard(String hostEmail, Long boardId) {
        // 게시판 정보 조회
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 호스트 권한 확인
        if (!board.getHostEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("게시판 삭제 권한이 없습니다.");
        }

        // 게시판 삭제 (소프트 삭제)
        boardMapper.deleteBoard(boardId);
    }

    /**
     * 게시판 상태 변경 (활성/보관)
     */
    @Transactional
    public BoardResponse updateBoardStatus(String hostEmail, Long boardId, String status) {
        // 게시판 정보 조회
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 호스트 권한 확인
        if (!board.getHostEmail().equals(hostEmail)) {
            throw new IllegalArgumentException("게시판 상태 변경 권한이 없습니다.");
        }

        // 상태 검증
        if (!status.equals("ACTIVE") && !status.equals("ARCHIVED")) {
            throw new IllegalArgumentException("유효하지 않은 상태입니다.");
        }

        // 게시판 상태 업데이트
        boardMapper.updateBoardStatus(boardId, status);

        // 업데이트된 정보 조회
        Board updatedBoard = boardMapper.findBoardById(boardId);
        return convertToResponse(updatedBoard);
    }

    /**
     * 게시판 호스트 변경
     */
    @Transactional
    public BoardResponse changeHost(String currentHostEmail, Long boardId, String newHostEmail) {
        // 게시판 정보 조회
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("존재하지 않는 게시판입니다.");
        }

        // 현재 호스트 확인
        if (!board.getHostEmail().equals(currentHostEmail)) {
            throw new IllegalArgumentException("호스트 변경 권한이 없습니다.");
        }

        // 새 호스트 사용자 확인
        User newHost = userMapper.findByEmail(newHostEmail);
        if (newHost == null) {
            throw new IllegalArgumentException("새 호스트 사용자를 찾을 수 없습니다.");
        }

        // 새 호스트가 현재 멤버인지 확인
        BoardMember newHostMember = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, newHostEmail);
        if (newHostMember == null || !newHostMember.getStatus().equals("ACTIVE")) {
            throw new IllegalArgumentException("새 호스트는 현재 활성 멤버여야 합니다.");
        }

        // 새 호스트의 역할을 HOST로 변경
        boardMemberMapper.updateMemberRole(newHostMember.getId(), "HOST");

        // 현재 호스트의 역할을 ADMIN으로 변경
        BoardMember currentHostMember = boardMemberMapper.findMemberByBoardIdAndUserEmail(boardId, currentHostEmail);
        boardMemberMapper.updateMemberRole(currentHostMember.getId(), "ADMIN");

        // 게시판의 호스트 이메일 업데이트
        board.setHostEmail(newHostEmail);
        boardMapper.updateBoard(board);

        // 업데이트된 정보 조회
        Board updatedBoard = boardMapper.findBoardById(boardId);
        return convertToResponse(updatedBoard);
    }

    /**
     * 게시판 멤버 목록 조회
     */
    public List<BoardMember> getBoardMembers(String userEmail, Long boardId) {
        // 게시판 존재 여부 확인
        Board board = boardMapper.findBoardById(boardId);
        if (board == null) {
            throw new IllegalArgumentException("게시판을 찾을 수 없습니다.");
        }
        
        // 권한 확인 (게시판 호스트 또는 관리자)
        boolean isHost = board.getHostEmail().equals(userEmail);
        boolean isAdmin = false;
        
        if (!isHost) {
            BoardMember member = boardMemberMapper.findBoardMemberByEmailAndBoardId(userEmail, boardId);
            if (member != null && "ADMIN".equals(member.getRole())) {
                isAdmin = true;
            }
        }
        
        if (!isHost && !isAdmin) {
            // 일반 멤버도 조회 가능하게 수정
            BoardMember currentUser = boardMemberMapper.findBoardMemberByEmailAndBoardId(userEmail, boardId);
            if (currentUser == null || !"ACTIVE".equals(currentUser.getStatus())) {
                throw new IllegalArgumentException("게시판 멤버 목록을 조회할 권한이 없습니다.");
            }
        }
        
        // 멤버 목록 조회
        return boardMemberMapper.findMembersByBoardId(boardId);
    }

    /**
     * Board 엔티티를 BoardResponse로 변환
     */
    private BoardResponse convertToResponse(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .imagePath(board.getImagePath())
                .hostEmail(board.getHostEmail())
                .hostName(board.getHostName())
                .memberCount(board.getMemberCount())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .status(board.getStatus())
                .build();
    }
}
