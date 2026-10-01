package com.example.demo.service;

import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.mapper.board.BoardMemberMapper;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardMember;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoardImageAccessTest {
    private final BoardMapper boards = mock(BoardMapper.class);
    private final BoardMemberMapper members = mock(BoardMemberMapper.class);
    private final BoardImageAccess access = new BoardImageAccess(boards, members);

    @Test
    void activeMemberCanReadButOnlyUploaderHostOrAdminCanDelete() {
        when(boards.findBoardById(30L)).thenReturn(Board.builder().id(30L)
                .hostEmail("host@haru.test").status("ACTIVE").build());
        when(members.findMemberByBoardIdAndUserEmail(30L, "member@haru.test"))
                .thenReturn(BoardMember.builder().status("ACTIVE").role("MEMBER").build());
        when(members.findMemberByBoardIdAndUserEmail(30L, "admin@haru.test"))
                .thenReturn(BoardMember.builder().status("ACTIVE").role("ADMIN").build());

        assertTrue(access.canRead(30L, "member@haru.test"));
        assertFalse(access.canDelete(30L, "member@haru.test", "uploader@haru.test"));
        assertTrue(access.canDelete(30L, "uploader@haru.test", "uploader@haru.test"));
        assertTrue(access.canDelete(30L, "host@haru.test", "uploader@haru.test"));
        assertTrue(access.canDelete(30L, "admin@haru.test", "uploader@haru.test"));
        assertFalse(access.canRead(30L, "outsider@haru.test"));
    }
}
