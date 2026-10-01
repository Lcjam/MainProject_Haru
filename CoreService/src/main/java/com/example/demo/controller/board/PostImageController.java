package com.example.demo.controller.board;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.mapper.board.BoardTempImageMapper;
import com.example.demo.service.BoardImageAccess;
import com.example.demo.service.FileStorageService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;

@RestController
@RequestMapping("/api/core/boards/{boardId}/posts/images")
@RequiredArgsConstructor
public class PostImageController {
    private final FileStorageService files;
    private final TokenUtils tokens;
    private final BoardImageAccess access;
    private final BoardTempImageMapper images;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadPostImage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long boardId, @RequestParam("image") MultipartFile image) {
        String email = tokens.getEmailFromAuthHeader(authorization);
        if (email == null) return ResponseEntity.status(401).body(ApiResponse.error("로그인이 필요합니다.", "AUTH_REQUIRED"));
        if (!access.canRead(boardId, email))
            return ResponseEntity.status(404).body(ApiResponse.error("이미지를 저장할 수 없습니다.", "NOT_FOUND"));

        String path;
        try {
            path = files.storeBoardFile(image, boardId, "image");
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage(), "BAD_REQUEST"));
        }
        if (path == null) return ResponseEntity.badRequest().body(ApiResponse.error("이미지 파일이 비어있습니다.", "BAD_REQUEST"));

        String fileId = path.substring(path.lastIndexOf('/') + 1);
        try {
            if (images.insert(boardId, fileId, email) != 1) throw new IllegalStateException("파일 메타데이터 저장 실패");
        } catch (RuntimeException exception) {
            files.deleteBoardFile(path);
            throw exception;
        }
        String imageUrl = "/api/core/boards/images/" + boardId + "/image/" + fileId;
        return ResponseEntity.ok(ApiResponse.success(Map.of("imageUrl", imageUrl, "fileId", fileId)));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<String>> deletePostImage(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long boardId, @RequestParam("fileId") String fileId) {
        String email = tokens.getEmailFromAuthHeader(authorization);
        if (email == null) return ResponseEntity.status(401).body(ApiResponse.error("로그인이 필요합니다.", "AUTH_REQUIRED"));
        String uploader = images.findUploader(boardId, fileId);
        if (uploader == null || !access.canDelete(boardId, email, uploader))
            return ResponseEntity.status(404).body(ApiResponse.error("이미지를 찾을 수 없습니다.", "NOT_FOUND"));

        String path = "/board-files/board_" + boardId + "/image/" + fileId;
        if (images.delete(boardId, fileId) != 1)
            return ResponseEntity.status(404).body(ApiResponse.error("이미지를 찾을 수 없습니다.", "NOT_FOUND"));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { files.deleteBoardFile(path); }
            });
        } else {
            files.deleteBoardFile(path);
        }
        return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다."));
    }
}
