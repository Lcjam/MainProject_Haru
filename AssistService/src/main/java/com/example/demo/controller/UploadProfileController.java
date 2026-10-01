package com.example.demo.controller;

import com.example.demo.dto.CommonResponseDTO;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assist/upload")
public class UploadProfileController {
    private final TokenUtils tokens;

    @PostMapping("/profile")
    public ResponseEntity<CommonResponseDTO<String>> uploadProfile(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestPart("file") MultipartFile file) {
        String error = tokens.authenticationErrorCode(authorization);
        if (error != null) return ResponseEntity.status(401).body(CommonResponseDTO.<String>builder()
                .status("error").code(error).message("로그인이 필요합니다.").build());

        return ResponseEntity.status(410).body(CommonResponseDTO.<String>builder()
                .status("error").code("UPLOAD_MOVED")
                .message("프로필 이미지는 Core 프로필 API로 업로드해 주세요.")
                .data(new CommonResponseDTO.Data<>("대체 경로", "/api/core/profiles/me/image"))
                .build());
    }
}
