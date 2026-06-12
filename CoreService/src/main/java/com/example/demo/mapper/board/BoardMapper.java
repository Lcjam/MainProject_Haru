package com.example.demo.mapper.board;

import com.example.demo.model.board.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BoardMapper {
    // 게시판 생성
    void createBoard(Board board);
    
    // ID로 게시판 조회
    Board findBoardById(@Param("id") Long id);
    
    // 모든 게시판 조회
    List<Board> findAllBoards();
    
    // 호스트의 게시판 조회
    List<Board> findBoardsByHostEmail(@Param("hostEmail") String hostEmail);
    
    // 사용자가 멤버인 게시판 조회
    List<Board> findBoardsByMemberEmail(@Param("memberEmail") String memberEmail);
    
    // 게시판 정보 업데이트
    void updateBoard(Board board);
    
    // 게시판 상태 변경
    void updateBoardStatus(@Param("id") Long id, @Param("status") String status);
    
    // 게시판 이미지 업데이트
    void updateBoardImage(@Param("id") Long id, @Param("imagePath") String imagePath);
    
    // 게시판 삭제 (소프트 삭제)
    void deleteBoard(@Param("id") Long id);
    
    // 게시판 멤버 수 조회
    int countBoardMembers(@Param("boardId") Long boardId);
}
