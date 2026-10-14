package com.example.demo.service;

import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.util.TokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ProfileImageRollbackTest {
    private final UserMapper users = mock(UserMapper.class);
    private final FileStorageService files = mock(FileStorageService.class);
    private final ProfileImageService service = new ProfileImageService(users, mock(TokenUtils.class), files);

    @Test
    void failedDatabaseUpdateDeletesNewImageAndKeepsOldOne() {
        var file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        when(users.findByEmail("owner@haru.test")).thenReturn(User.builder()
                .email("owner@haru.test").profileImagePath("old.png").build());
        when(files.storeProfileImage(file, "owner@haru.test")).thenReturn("new.png");
        when(users.updateProfileImagePath("owner@haru.test", "new.png"))
                .thenThrow(new IllegalStateException("database failed"));

        assertThrows(IllegalStateException.class,
                () -> service.uploadProfileImage("owner@haru.test", file));
        verify(files).deleteProfileImage("new.png");
        verify(files, never()).deleteProfileImage("old.png");
    }

    @Test
    void failedDeleteKeepsCurrentImage() {
        when(users.findByEmail("owner@haru.test")).thenReturn(User.builder()
                .email("owner@haru.test").profileImagePath("old.png").build());
        when(users.updateProfileImagePath("owner@haru.test", null))
                .thenThrow(new IllegalStateException("database failed"));

        assertThrows(IllegalStateException.class,
                () -> service.deleteProfileImage("owner@haru.test"));
        verify(files, never()).deleteProfileImage("old.png");
    }
}
