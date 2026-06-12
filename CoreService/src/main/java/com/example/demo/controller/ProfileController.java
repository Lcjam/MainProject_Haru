package com.example.demo.controller;

import com.example.demo.dto.response.*;
import com.example.demo.dto.profile.*;
import com.example.demo.dto.auth.*;
import com.example.demo.dto.hobby.*;
import com.example.demo.service.FileStorageService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/core/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {
    
    private final UserService userService;
    private final FileStorageService fileStorageService;
    
    /**
     * 자신의 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(@RequestHeader("Authorization") String token) {
        ProfileResponse profile = userService.getUserProfileByToken(token);
        
        if (!profile.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(profile, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    /**
     * 닉네임으로 다른 사용자의 프로필 조회 (공개 정보만)
     */
    @GetMapping("/user/{nickname}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getUserProfile(@PathVariable String nickname) {
        ProfileResponse profile = userService.getPublicProfile(nickname);
        
        if (!profile.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(profile, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    /**
     * 이메일로 사용자 프로필 조회 (관리자 기능)
     */
    @GetMapping("/admin/user/{email}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getAdminUserProfile(
            @RequestHeader("Authorization") String token,
            @PathVariable String email) {
        
        // 주의: 실제 구현에서는 여기에 관리자 권한 검증 필요
        ProfileResponse profile = userService.getUserProfile(email);
        
        if (!profile.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(profile, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    /**
     * 자신의 프로필 수정
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateMyProfile(
            @RequestHeader("Authorization") String token,
            @RequestBody ProfileUpdateRequest request) {
        
        // 취미 정보의 기본 유효성 검증
        if (request.getHobbies() != null && !request.getHobbies().isEmpty()) {
            for (HobbyRequest hobby : request.getHobbies()) {
                if (hobby.getCategoryId() == null || hobby.getHobbyId() == null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(
                        ProfileUpdateResponse.builder()
                            .success(false)
                            .message("카테고리 또는 취미 정보가 누락되었습니다.")
                            .build(),
                        "400"
                    ));
                }
            }
        }
        
        ProfileUpdateResponse response = userService.updateProfileByToken(token, request);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 비밀번호 변경
     */
    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody PasswordChangeRequest request) {
        
        PasswordChangeResponse response = userService.changePasswordByToken(token, request);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @PostMapping("/me/image")
    public ResponseEntity<ApiResponse<ProfileImageResponse>> uploadProfileImage(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file) {
        
        // 파일 기본 검증
        if (file.isEmpty()) {
            ProfileImageResponse errorResponse = ProfileImageResponse.builder()
                    .success(false)
                    .message("업로드할 파일이 비어있습니다.")
                    .build();
            return ResponseEntity.badRequest().body(ApiResponse.error(errorResponse, "400"));
        }
        
        try {
            ProfileImageResponse response = userService.uploadProfileImageByToken(token, file);
            
            if (!response.isSuccess()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
            }
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 파일 형식이나 크기 검증 실패 시
            ProfileImageResponse errorResponse = ProfileImageResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(ApiResponse.error(errorResponse, "400"));
        } catch (Exception e) {
            log.error("이미지 업로드 중 예상치 못한 오류 발생: {}", e.getMessage());
            ProfileImageResponse errorResponse = ProfileImageResponse.builder()
                    .success(false)
                    .message("이미지 업로드 중 서버 오류가 발생했습니다.")
                    .build();
            return ResponseEntity.status(500).body(ApiResponse.error(errorResponse, "500"));
        }
    }
    
    /**
     * 프로필 이미지 삭제
     */
    @DeleteMapping("/me/image")
    public ResponseEntity<ApiResponse<ProfileImageResponse>> deleteProfileImage(
            @RequestHeader("Authorization") String token) {
        
        ProfileImageResponse response = userService.deleteProfileImageByToken(token);
        
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(response, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 프로필 이미지 정보 조회
     */
    @GetMapping("/me/image-info")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMyProfileImageInfo(
            @RequestHeader("Authorization") String token) {
        
        String tokenWithoutBearer = token;
        if (token.startsWith("Bearer ")) {
            tokenWithoutBearer = token.substring(7);
        }
        
        if (!userService.tokenUtils.isTokenValid(tokenWithoutBearer)) {
            Map<String, String> errorData = new HashMap<>();
            errorData.put("message", "유효하지 않은 인증 토큰입니다.");
            return ResponseEntity.badRequest().body(ApiResponse.error(errorData, "400"));
        }
        
        String email = userService.tokenUtils.getEmailFromToken(tokenWithoutBearer);
        String imageUrl = userService.getProfileImageUrl(email);
        
        Map<String, String> responseData = new HashMap<>();
        responseData.put("imageUrl", imageUrl);
        
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
    
    /**
     * 프로필 이미지 파일 제공 
     * 정적 리소스로 접근하도록 리다이렉션 처리
     */
    @GetMapping("/image/{filename:.+}")
    public ResponseEntity<?> getProfileImage(@PathVariable String filename) {
        try {
            // 파일 경로 생성
            java.nio.file.Path imagePath = java.nio.file.Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/profile-images/" + filename);
            java.io.File file = imagePath.toFile();
            System.out.println("@@@@@@@@@@@@@@@@@@@파일 경로: " + imagePath);
            
            // 파일이 존재하는지 확인
            if (!file.exists()) {
                log.error("프로필 이미지 파일을 찾을 수 없음: {}", filename);
                return ResponseEntity.notFound().build();
            }
            
            // 파일 내용 읽기
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toURI());
            
            // MIME 타입 감지
            String contentType = java.nio.file.Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // 이미지 직접 반환
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception ex) {
            log.error("프로필 이미지 제공 실패: {}", ex.getMessage());
            return ResponseEntity.status(500).body("이미지 로드 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 기본 프로필 이미지 반환
     * 정적 리소스로 접근하도록 리다이렉션 처리
     */
    @GetMapping("/image/default")
    public ResponseEntity<?> getDefaultProfileImage() {
        try {
            String defaultImageName = fileStorageService.getDefaultProfileImageName();
            
            // 파일 경로 생성
            java.nio.file.Path imagePath = java.nio.file.Paths.get(System.getProperty("user.dir") + "/src/main/resources/static/profile-images/" + defaultImageName);
            java.io.File file = imagePath.toFile();
            
            // 파일이 존재하는지 확인
            if (!file.exists()) {
                log.error("기본 프로필 이미지 파일을 찾을 수 없음: {}", defaultImageName);
                return ResponseEntity.notFound().build();
            }
            
            // 파일 내용 읽기
            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(file.toURI());
            
            // MIME 타입 감지
            String contentType = java.nio.file.Files.probeContentType(imagePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // 이미지 직접 반환
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception ex) {
            log.error("기본 프로필 이미지 제공 실패: {}", ex.getMessage());
            return ResponseEntity.status(500).body("이미지 로드 중 오류가 발생했습니다.");
        }
    }
    
    /**
     * 마이페이지 정보 조회
     */
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPageInfo(@RequestHeader("Authorization") String token) {
        MyPageResponse myPage = userService.getMyPageInfoByToken(token);
        
        if (!myPage.isSuccess()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(myPage, "400"));
        }
        
        return ResponseEntity.ok(ApiResponse.success(myPage));
    }
}
