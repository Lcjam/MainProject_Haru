package com.example.demo.controller.board;

import com.example.demo.mapper.board.BoardTempImageMapper;
import com.example.demo.service.BoardImageAccess;
import com.example.demo.service.FileStorageService;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PostImageAccessTest {
    private final FileStorageService files = mock(FileStorageService.class);
    private final TokenUtils tokens = mock(TokenUtils.class);
    private final BoardImageAccess access = mock(BoardImageAccess.class);
    private final BoardTempImageMapper images = mock(BoardTempImageMapper.class);
    private final PostImageController controller = new PostImageController(files, tokens, access, images);
    private final MockMultipartFile image = new MockMultipartFile("image", "a.png", "image/png", new byte[]{1});

    @Test
    void outsiderCannotUploadOrDeleteOtherUsersFile() {
        when(tokens.getEmailFromAuthHeader("Bearer other")).thenReturn("other@haru.test");
        assertEquals(404, controller.uploadPostImage("Bearer other", 30L, image).getStatusCode().value());
        when(images.findUploader(30L, "file.png")).thenReturn("owner@haru.test");
        assertEquals(404, controller.deletePostImage("Bearer other", 30L, "file.png").getStatusCode().value());
        verifyNoInteractions(files);
    }

    @Test
    void databaseFailureRemovesNewFile() {
        when(tokens.getEmailFromAuthHeader("Bearer owner")).thenReturn("owner@haru.test");
        when(access.canRead(30L, "owner@haru.test")).thenReturn(true);
        when(files.storeBoardFile(eq(image), eq(30L), eq("image")))
                .thenReturn("/board-files/board_30/image/file.png");
        when(images.insert(30L, "file.png", "owner@haru.test"))
                .thenThrow(new IllegalStateException("db failed"));

        assertThrows(IllegalStateException.class,
                () -> controller.uploadPostImage("Bearer owner", 30L, image));
        verify(files).deleteBoardFile("/board-files/board_30/image/file.png");
    }

    @Test
    void anotherBoardMemberCannotReadUnknownFile() {
        when(tokens.getEmailFromAuthHeader("Bearer member")).thenReturn("member@haru.test");
        when(access.canRead(30L, "member@haru.test")).thenReturn(true);
        var viewer = new PostImageViewerController(files, tokens, access, images);

        assertEquals(404, viewer.getImage("Bearer member", 30L, "image", "unknown.png")
                .getStatusCode().value());
        verifyNoInteractions(files);
    }

    @Test
    void databaseDeleteFailureKeepsFile() {
        when(tokens.getEmailFromAuthHeader("Bearer owner")).thenReturn("owner@haru.test");
        when(images.findUploader(30L, "file.png")).thenReturn("owner@haru.test");
        when(access.canDelete(30L, "owner@haru.test", "owner@haru.test")).thenReturn(true);
        when(images.delete(30L, "file.png")).thenThrow(new IllegalStateException("db failed"));

        assertThrows(IllegalStateException.class, () -> controller.deletePostImage("Bearer owner", 30L, "file.png"));
        verifyNoInteractions(files);
    }
}
