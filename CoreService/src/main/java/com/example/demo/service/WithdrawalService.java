package com.example.demo.service;

import com.example.demo.dto.auth.WithdrawalRequest;
import com.example.demo.dto.auth.WithdrawalResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.PasswordUtils;
import com.example.demo.util.RandomUtils;
import com.example.demo.util.TokenUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {
    
    private final UserMapper userMapper;
    private final TokenUtils tokenUtils;
    private final PasswordUtils passwordUtils;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final FileStorageService fileStorageService;

    /**
     * 회원 탈퇴 처리 (소프트 삭제)
     */
    @Transactional
    public WithdrawalResponse withdrawUser(String email, WithdrawalRequest request) {
        // 사용자 존재 여부 확인
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return WithdrawalResponse.builder()
                    .success(false)
                    .message("존재하지 않는 사용자입니다.")
                    .build();
        }
        
        // 소셜 로그인 사용자이거나, 이메일 로그인이면 비밀번호 확인
        if ("EMAIL".equals(user.getLoginMethod())) {
            // 비밀번호 확인
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return WithdrawalResponse.builder()
                        .success(false)
                        .message("비밀번호를 입력해주세요.")
                        .build();
            }
            
            boolean isPasswordValid = passwordUtils.verifyPassword(
                    request.getPassword(),
                    user.getPasswordHash(),
                    null
            );
            
            if (!isPasswordValid) {
                return WithdrawalResponse.builder()
                        .success(false)
                        .message("비밀번호가 일치하지 않습니다.")
                        .build();
            }
        }
        
        // 현재 시간
        LocalDateTime now = LocalDateTime.now();
        
        // 개인정보 익명화 처리
        // 1. 무작위 코드 생성 (10자리)
        String randomCode = RandomUtils.generateRandomString(10);
        
        // 2. 닉네임 변경: "탈퇴회원_" + 무작위코드
        String newNickname = "탈퇴회원_" + randomCode;
        
        // 3. 휴대폰번호 마스킹
        String maskedPhoneNumber = RandomUtils.maskPhoneNumber(user.getPhoneNumber());
        
        // 4. 자기소개 변경
        String newBio = "탈퇴한 회원입니다.";
        
        // 5. 프로필 이미지 처리 - 기존 이미지가 있으면 삭제
        if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
            fileStorageService.deleteProfileImage(user.getProfileImagePath());
        }
        
        // 사용자 정보 익명화 및 탈퇴 처리
        userMapper.anonymizeUserData(email, newNickname, maskedPhoneNumber, newBio, now);
        
        // 계정 정보 테이블의 상태도 업데이트
        userMapper.updateUserAccountInfoStatus(email, "Withdrawal");
        
        // 응답 생성
        return WithdrawalResponse.builder()
                .success(true)
                .message("회원 탈퇴가 완료되었습니다.")
                .withdrawalDate(now)
                .build();
    }
    
    /**
     * 토큰을 통한 회원 탈퇴 처리
     */
    @Transactional
    public WithdrawalResponse withdrawUserByToken(String token, WithdrawalRequest request) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return WithdrawalResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }
        
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        
        WithdrawalResponse response = withdrawUser(email, request);
        
        // 성공적으로 탈퇴한 경우, 토큰을 블랙리스트에 추가
        if (response.isSuccess()) {
            try {
                // 토큰 만료 시간 가져오기
                java.util.Date expiration = jwtTokenProvider.getExpirationDate(tokenWithoutBearer);
                Long expiryMillis = expiration.getTime();
                
                // 블랙리스트에 추가
                jwtTokenBlacklistService.addToBlacklist(tokenWithoutBearer, expiryMillis);
                
                log.info("회원 탈퇴: 이메일={}, 토큰이 블랙리스트에 추가되었습니다.", email);
            } catch (Exception e) {
                log.error("회원 탈퇴 후 토큰 블랙리스팅 중 오류 발생: {}", e.getMessage());
            }
        }
        
        return response;
    }
}
