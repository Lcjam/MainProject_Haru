package com.example.demo.controller;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.dto.CommonResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/assist/upload")
@Slf4j
public class UploadProfileController {
    @PostMapping("/profile")
    public ResponseEntity<CommonResponseDTO> uploadProfile(@RequestPart("file") MultipartFile file) {
        log.debug("Profile uploaded successfully");
        return ResponseEntity.ok(CommonResponseDTO.builder()
                .status("success")
                .data(new CommonResponseDTO.Data("Profile uploaded successfully", ""))
                .code("200")
                .build());
    }
}