package com.example.demo.mapper.board;

import com.example.demo.model.board.BoardMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface BoardMemberMapper {
    
    // 멤버 추가
    void addMember(BoardMember member);
    
    // 게시판 멤버 목록 조회
    List<BoardMember> findMembersByBoardId(@Param("boardId") Long boardId);
    
    // 특정 멤버 조회
    BoardMember findMemberById(@Param("id") Long id);
    
    // 특정 사용자의 게시판 멤버십 조회
    BoardMember findMemberByBoardIdAndUserEmail(
            @Param("boardId") Long boardId, 
            @Param("userEmail") String userEmail);
    
    // 멤버 역할 업데이트
    void updateMemberRole(
            @Param("id") Long id, 
            @Param("role") String role);
    
    // 멤버 상태 업데이트
    void updateMemberStatus(
            @Param("id") Long id, 
            @Param("status") String status);
    
    // 멤버 삭제
    void deleteMember(@Param("id") Long id);
    
    // 게시판의 모든 멤버 삭제
    void deleteAllMembersByBoardId(@Param("boardId") Long boardId);
    
    /**
     * 이메일과 게시판 ID로 게시판 멤버 조회
     */
    BoardMember findBoardMemberByEmailAndBoardId(
            @Param("email") String email, 
            @Param("boardId") Long boardId);
}
