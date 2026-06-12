package com.example.demo.controller.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/core/boards/images")
@RequiredArgsConstructor
@Slf4j
public class PostImageViewerController {

    /**
     * 게시글 이미지 조회
     */
    @GetMapping("/{boardId}/{fileType}/{fileName:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable Long boardId,
            @PathVariable String fileType,
            @PathVariable String fileName) {
        
        try {
            // 이미지 파일 경로 생성
            String imagePath = "board-files/board_" + boardId + "/" + fileType + "/" + fileName;
            Path filePath = Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/" + imagePath);
            
            // 파일이 존재하는지 확인
            if (!Files.exists(filePath)) {
                log.error("이미지 파일을 찾을 수 없음: {}", imagePath);
                return ResponseEntity.notFound().build();
            }
            
            // 리소스 생성
            Resource resource = new UrlResource(filePath.toUri());
            
            // MIME 타입 감지
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // 이미지 직접 반환
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
            
        } catch (IOException e) {
            log.error("이미지 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
