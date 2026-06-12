package com.example.demo.controller.board;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.service.FileStorageService;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/core/boards/{boardId}/posts/images")
@RequiredArgsConstructor
@Slf4j
public class PostImageController {

    private final FileStorageService fileStorageService;
    private final TokenUtils tokenUtils;

    /**
     * 게시글 이미지 업로드
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> uploadPostImage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestParam("image") MultipartFile image) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // 파일 검증
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("이미지 파일이 비어있습니다.", "400"));
            }
            
            // 이미지 저장
            String fileName = image.getOriginalFilename();
            String imagePath = fileStorageService.storeBoardFile(image, boardId, "image");
            
            if (imagePath == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("이미지 저장에 실패했습니다.", "400"));
            }
            
            // 이미지 URL 생성 - API 엔드포인트로 변경
            String imageUrl = "/api/core/boards/images/" + boardId + "/image/" + 
                              imagePath.substring(imagePath.lastIndexOf("/") + 1);
            
            // 응답 데이터
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            response.put("originalFileName", fileName);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("이미지 업로드 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }

    /**
     * 게시글 이미지 삭제
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> deletePostImage(
            @RequestHeader("Authorization") String token,
            @PathVariable Long boardId,
            @RequestParam("imageUrl") String imageUrl) {
        
        String email = tokenUtils.getEmailFromAuthHeader(token);
        
        if (email == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("인증되지 않은 요청입니다.", "401"));
        }
        
        try {
            // 이미지 경로 검증
            if (!imageUrl.startsWith("/board-files/board_" + boardId)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("유효하지 않은 이미지 URL입니다.", "400"));
            }
            
            // 이미지 삭제
            boolean deleted = fileStorageService.deleteBoardFile(imageUrl);
            
            if (!deleted) {
                return ResponseEntity.badRequest().body(ApiResponse.error("이미지 삭제에 실패했습니다.", "400"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("이미지가 삭제되었습니다."));
        } catch (Exception e) {
            log.error("이미지 삭제 중 오류: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("서버 오류가 발생했습니다.", "500"));
        }
    }
}
