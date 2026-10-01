package com.example.demo.service;

import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardImageAccess {
    private final BoardMapper boards;
    private final BoardMemberMapper members;

    public boolean canRead(Long boardId, String email) {
        Board board = boards.findBoardById(boardId);
        if (board == null || "DELETED".equals(board.getStatus()) || email == null) return false;
        if (email.equals(board.getHostEmail())) return true;
        BoardMember member = members.findMemberByBoardIdAndUserEmail(boardId, email);
        return member != null && "ACTIVE".equals(member.getStatus());
    }

    public boolean canDelete(Long boardId, String email, String uploader) {
        Board board = boards.findBoardById(boardId);
        if (board == null || "DELETED".equals(board.getStatus()) || email == null) return false;
        if (email.equals(uploader) || email.equals(board.getHostEmail())) return true;
        BoardMember member = members.findMemberByBoardIdAndUserEmail(boardId, email);
        return member != null && "ACTIVE".equals(member.getStatus()) && "ADMIN".equals(member.getRole());
    }
}
