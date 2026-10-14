package com.example.demo.service.Market;

import com.example.demo.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageUploadService {
    private final FileStorageService files;

    public ResponseEntity<Object> uploadProductImages(String email, Long productId, List<MultipartFile> images) {
        if (images == null || images.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("message", "업로드된 파일이 없습니다."));
        List<String> paths = new ArrayList<>();
        try {
            for (MultipartFile image : images) {
                String path = files.storeFile(image, "product-images/" + productId, image.getOriginalFilename());
                if (path == null) throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
                paths.add(path);
            }
            return ResponseEntity.ok(Map.of("status", 200, "imagePaths", paths));
        } catch (RuntimeException exception) {
            deleteProductImages(paths);
            throw exception;
        }
    }

    public void deleteProductImages(List<String> paths) {
        paths.forEach(files::deleteStoredFile);
    }

    public Path resolveImagePath(String path) {
        return files.resolveStoredPath(path);
    }
}
