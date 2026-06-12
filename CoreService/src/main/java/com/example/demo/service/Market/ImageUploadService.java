package com.example.demo.service.Market;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageUploadService {
    // **파일 저장 경로를 `/src/main/resources/static/uploads/`로 설정**
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    public ResponseEntity<Object> uploadProductImages(String email, Long productId, List<MultipartFile> files) {
        List<String> uploadedPaths = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", "Bad Request",
                    "message", "업로드된 파일이 없습니다."
            ));
        }

        String productDir = UPLOAD_DIR + "product_" + email + "/product_" + productId + "/";
        File uploadDir = new File(productDir);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        try {
            for (MultipartFile file : files) {
                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = Paths.get(productDir, fileName);
                file.transferTo(filePath.toFile());

                String imageUrl = "/uploads/product_" + email + "/product_" + productId + "/" + fileName;
                
                //  업로드된 이미지 경로를 리스트에 추가
                uploadedPaths.add(imageUrl);
            }
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", 500,
                    "error", "Internal Server Error",
                    "message", "이미지 업로드 중 오류 발생: " + e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "이미지가 성공적으로 업로드되었습니다.",
                "imagePaths", uploadedPaths
        ));
    }
}

