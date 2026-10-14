package com.example.demo.controller.board;

import com.example.demo.mapper.board.BoardTempImageMapper;
import com.example.demo.service.BoardImageAccess;
import com.example.demo.service.FileStorageService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/core/boards/images")
@RequiredArgsConstructor
public class PostImageViewerController {
    private final FileStorageService files;
    private final TokenUtils tokens;
    private final BoardImageAccess access;
    private final BoardTempImageMapper images;

    @GetMapping("/{boardId}/{fileType}/{fileName:.+}")
    public ResponseEntity<Resource> getImage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long boardId, @PathVariable String fileType, @PathVariable String fileName) {
        String email = tokens.getEmailFromAuthHeader(authorization);
        if (email == null) return ResponseEntity.status(401).build();
        if (!"image".equals(fileType) || !access.canRead(boardId, email)
                || (images.findUploader(boardId, fileName) == null && !files.isLegacyBoardFileName(fileName))) return ResponseEntity.notFound().build();
        try {
            Path path = files.resolveStoredPath("/board-files/board_" + boardId + "/image/" + fileName);
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(Files.probeContentType(path)))
                    .body(resource);
        } catch (Exception exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
