package com.example.demo.service;

import com.example.demo.dto.profile.ProfileImageResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.util.TokenUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImageService {
    
    private final UserMapper userMapper;
    private final TokenUtils tokenUtils;
    private final FileStorageService fileStorageService;
    
    /**
     * 프로필 이미지 URL 조회 - static 폴더의 프로필 이미지로 접근하도록 변경
     */
    public String getProfileImageUrl(String email) {
        User user = userMapper.findByEmail(email);
        
        if (user != null && user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
            // 정적 리소스로 접근하도록 URL 구성 변경
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/profile-images/")
                    .path(user.getProfileImagePath())
                    .toUriString();
        }
        
        // 기본 이미지 URL도 정적 리소스로 접근
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/profile-images/")
                .path(fileStorageService.getDefaultProfileImageName())
                .toUriString();
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @Transactional
    public ProfileImageResponse uploadProfileImage(String email, MultipartFile file) {
        try {
            // 사용자 존재 여부 확인
            User user = userMapper.findByEmail(email);
            if (user == null) {
                return ProfileImageResponse.builder()
                        .success(false)
                        .message("존재하지 않는 사용자입니다.")
                        .build();
            }
            
            // 기존 이미지가 있으면 삭제
            if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
                fileStorageService.deleteProfileImage(user.getProfileImagePath());
            }
            
            // 새 이미지 저장
            String fileName = fileStorageService.storeProfileImage(file, email);
            
            if (fileName == null) {
                return ProfileImageResponse.builder()
                        .success(false)
                        .message("이미지 저장에 실패했습니다.")
                        .build();
            }
            
            // DB에 파일 경로 업데이트
            userMapper.updateProfileImagePath(email, fileName);
            
            // 이미지 URL 생성
            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/core/profiles/image/")
                    .path(fileName)
                    .toUriString();
            
            return ProfileImageResponse.builder()
                    .success(true)
                    .message("프로필 이미지가 성공적으로 업로드되었습니다.")
                    .profileImagePath(fileName)
                    .profileImageUrl(fileDownloadUri)
                    .build();
            
        } catch (Exception e) {
       
            return ProfileImageResponse.builder()
                    .success(false)
                    .message("프로필 이미지 업로드 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 프로필 이미지 삭제
     */
    @Transactional
    public ProfileImageResponse deleteProfileImage(String email) {
        try {
            // 사용자 존재 여부 확인
            User user = userMapper.findByEmail(email);
            if (user == null) {
                return ProfileImageResponse.builder()
                        .success(false)
                        .message("존재하지 않는 사용자입니다.")
                        .build();
            }
            
            // 이미지가 있으면 삭제
            if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
                fileStorageService.deleteProfileImage(user.getProfileImagePath());
                userMapper.updateProfileImagePath(email, null);
                
                return ProfileImageResponse.builder()
                        .success(true)
                        .message("프로필 이미지가 성공적으로 삭제되었습니다.")
                        .build();
            } else {
                return ProfileImageResponse.builder()
                        .success(false)
                        .message("삭제할 프로필 이미지가 없습니다.")
                        .build();
            }
            
        } catch (Exception e) {
        
            return ProfileImageResponse.builder()
                    .success(false)
                    .message("프로필 이미지 삭제 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
    
    /**
     * 토큰을 통한 프로필 이미지 업로드
     */
    public ProfileImageResponse uploadProfileImageByToken(String token, MultipartFile file) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ProfileImageResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        return uploadProfileImage(email, file);
    }
    
    /**
     * 토큰을 통한 프로필 이미지 삭제
     */
    public ProfileImageResponse deleteProfileImageByToken(String token) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ProfileImageResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        return deleteProfileImage(email);
    }
}
