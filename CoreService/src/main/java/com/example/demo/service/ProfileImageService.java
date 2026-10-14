package com.example.demo.service;

import com.example.demo.dto.profile.ProfileImageResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.util.TokenUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/core/profiles/image/")
                    .path(user.getProfileImagePath())
                    .toUriString();
        }
        
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/core/profiles/image/default")
                .toUriString();
    }
    
    /**
     * 프로필 이미지 업로드
     */
    @Transactional
    public ProfileImageResponse uploadProfileImage(String email, MultipartFile file) {
        User user = userMapper.findByEmail(email);
        if (user == null) return ProfileImageResponse.builder().success(false)
                .message("존재하지 않는 사용자입니다.").build();

        String fileName = fileStorageService.storeProfileImage(file, email);
        if (fileName == null) return ProfileImageResponse.builder().success(false)
                .message("이미지 저장에 실패했습니다.").build();
        String oldFileName = user.getProfileImagePath();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) fileStorageService.deleteProfileImage(oldFileName);
                    else fileStorageService.deleteProfileImage(fileName);
                }
            });
        }
        try {
            if (userMapper.updateProfileImagePath(email, fileName) != 1)
                throw new IllegalStateException("프로필 이미지 DB 갱신 실패");
        } catch (RuntimeException exception) {
            if (!TransactionSynchronizationManager.isSynchronizationActive())
                fileStorageService.deleteProfileImage(fileName);
            throw exception;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive())
            fileStorageService.deleteProfileImage(oldFileName);

        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/core/profiles/image/").path(fileName).toUriString();
        return ProfileImageResponse.builder().success(true)
                .message("프로필 이미지가 성공적으로 업로드되었습니다.")
                .profileImagePath(fileName).profileImageUrl(fileDownloadUri).build();
    }
    
    /**
     * 프로필 이미지 삭제
     */
    @Transactional
    public ProfileImageResponse deleteProfileImage(String email) {
        User user = userMapper.findByEmail(email);
        if (user == null) return ProfileImageResponse.builder().success(false)
                .message("존재하지 않는 사용자입니다.").build();
        String oldFileName = user.getProfileImagePath();
        if (oldFileName == null || oldFileName.isEmpty()) return ProfileImageResponse.builder()
                .success(false).message("삭제할 프로필 이미지가 없습니다.").build();
        if (userMapper.updateProfileImagePath(email, null) != 1)
            throw new IllegalStateException("프로필 이미지 DB 갱신 실패");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { fileStorageService.deleteProfileImage(oldFileName); }
            });
        } else fileStorageService.deleteProfileImage(oldFileName);
        return ProfileImageResponse.builder().success(true)
                .message("프로필 이미지가 성공적으로 삭제되었습니다.").build();
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
