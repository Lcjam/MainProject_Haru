package com.example.demo.controller.board;

import com.example.demo.mapper.board.BoardMapper;
import com.example.demo.model.board.Board;
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
@RequiredArgsConstructor
@RequestMapping("/api/core/boards/{boardId}/image")
public class BoardImageViewerController {
    private final TokenUtils tokens;
    private final BoardImageAccess access;
    private final BoardMapper boards;
    private final FileStorageService files;

    @GetMapping("/{fileId:.+}")
    public ResponseEntity<Resource> getImage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long boardId, @PathVariable String fileId) {
        String email = tokens.getEmailFromAuthHeader(authorization);
        if (email == null) return ResponseEntity.status(401).build();
        if (!access.canRead(boardId, email)) return ResponseEntity.notFound().build();
        Board board = boards.findBoardById(boardId);
        String relativePath = "/board-images/" + boardId + "/" + fileId;
        if (board == null || !relativePath.equals(board.getImagePath())) return ResponseEntity.notFound().build();
        try {
            Path path = files.resolveStoredPath(relativePath);
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) return ResponseEntity.notFound().build();
            String type = Files.probeContentType(path);
            if (type == null || !type.startsWith("image/")) return ResponseEntity.notFound().build();
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).body(resource);
        } catch (Exception exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
