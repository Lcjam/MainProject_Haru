package com.example.demo.service;

import com.example.demo.dto.auth.*;
import com.example.demo.dto.profile.*;
import com.example.demo.dto.hobby.HobbyRequest;
import com.example.demo.model.UserHobby;
import com.example.demo.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final AuthService authService;
    private final HobbyService hobbyService;
    private final ProfileService profileService;
    private final ProfileImageService profileImageService;
    private final WithdrawalService withdrawalService;

    // 컨트롤러에서 접근 필요
    public final TokenUtils tokenUtils;

    /**
     * 회원 가입
     */
    public SignupResponse registerUser(SignupRequest request) {
        return authService.registerUser(request);
    }

    /**
     * 로그인
     */
    public LoginResponse login(LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 로그아웃
     */
    public LogoutResponse logout(String token) {
        return authService.logout(token);
    }

    /**
     * 사용자 취미 정보 조회
     */
    public List<UserHobby> getUserHobbies(String email) {
        return hobbyService.getUserHobbies(email);
    }

    /**
     * 사용자 취미 정보 업데이트
     */
    public void updateUserHobbies(String email, List<HobbyRequest> hobbies) {
        hobbyService.registerUserHobbies(email, hobbies);
    }

    /**
     * 사용자 프로필 조회
     */
    public ProfileResponse getUserProfile(String email) {
        return profileService.getUserProfile(email);
    }

    /**
     * 토큰으로 사용자 프로필 조회
     */
    public ProfileResponse getUserProfileByToken(String token) {
        return profileService.getUserProfileByToken(token);
    }

    /**
     * 닉네임으로 사용자 프로필 조회 (공개 정보만)
     */
    public ProfileResponse getPublicProfile(String nickname) {
        return profileService.getPublicProfile(nickname);
    }

    /**
     * 사용자 프로필 업데이트
     */
    public ProfileUpdateResponse updateProfile(String email, ProfileUpdateRequest request) {
        return profileService.updateProfile(email, request);
    }

    /**
     * 토큰을 통한 사용자 프로필 업데이트
     */
    public ProfileUpdateResponse updateProfileByToken(String token, ProfileUpdateRequest request) {
        return profileService.updateProfileByToken(token, request);
    }

    /**
     * 비밀번호 변경
     * 토큰 없이 비번 변경경
     */
    public PasswordChangeResponse changePassword(PasswordChangeRequest request) {
        return profileService.changePassword(request.getEmail(), request);
    }

    /**
     * 토큰을 통한 비밀번호 변경
     * 여기로 와집니다 제가 어제 한거랑 다른 곳이죠죠
     */
    public PasswordChangeResponse changePasswordByToken(String token, PasswordChangeRequest request) {
        return profileService.changePasswordByToken(token, request);
    }

    /**
     * 회원 탈퇴 처리
     */
    public WithdrawalResponse withdrawUser(String email, WithdrawalRequest request) {
        return withdrawalService.withdrawUser(email, request);
    }

    /**
     * 토큰을 통한 회원 탈퇴 처리
     */
    public WithdrawalResponse withdrawUserByToken(String token, WithdrawalRequest request) {
        return withdrawalService.withdrawUserByToken(token, request);
    }

    /**
     * 프로필 이미지 업로드
     */
    public ProfileImageResponse uploadProfileImage(String email, MultipartFile file) {
        return profileImageService.uploadProfileImage(email, file);
    }

    /**
     * 프로필 이미지 삭제
     */
    public ProfileImageResponse deleteProfileImage(String email) {
        return profileImageService.deleteProfileImage(email);
    }

    /**
     * 프로필 이미지 URL 조회
     */
    public String getProfileImageUrl(String email) {
        return profileImageService.getProfileImageUrl(email);
    }

    /**
     * 토큰을 통한 프로필 이미지 업로드
     */
    public ProfileImageResponse uploadProfileImageByToken(String token, MultipartFile file) {
        return profileImageService.uploadProfileImageByToken(token, file);
    }

    /**
     * 토큰을 통한 프로필 이미지 삭제
     */
    public ProfileImageResponse deleteProfileImageByToken(String token) {
        return profileImageService.deleteProfileImageByToken(token);
    }

    /**
     * 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageInfo(String email) {
        return profileService.getMyPageInfo(email);
    }

    /**
     * 토큰으로 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageInfoByToken(String token) {
        return profileService.getMyPageInfoByToken(token);
    }
}
