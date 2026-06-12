package com.example.demo.service;

import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.LoginResponse;
import com.example.demo.dto.auth.LogoutResponse;
import com.example.demo.dto.auth.SignupRequest;
import com.example.demo.dto.auth.SignupResponse;
import com.example.demo.dto.hobby.HobbyRequest;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenBlacklistService;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordUtils passwordUtils;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenBlacklistService jwtTokenBlacklistService;
    private final HobbyService hobbyService;  // 추가된 의존성

    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * 회원가입 - 카테고리->취미 선택 방식으로 수정
     */
    @Transactional
    public SignupResponse registerUser(SignupRequest request) {
        try {
            // 이메일 중복 체크
            if (userMapper.findByEmail(request.getEmail()) != null) {
                return SignupResponse.builder()
                        .success(false)
                        .message("이미 존재하는 이메일입니다.")
                        .build();
            }

            // 닉네임 중복 체크
            if (userMapper.findByNickname(request.getNickname()) != null) {
                return SignupResponse.builder()
                        .success(false)
                        .message("이미 존재하는 닉네임입니다.")
                        .build();
            }

            // 전화번호 중복 체크 (선택 사항)
            if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
                if (userMapper.findByPhoneNumber(request.getPhoneNumber()) != null) {
                    return SignupResponse.builder()
                            .success(false)
                            .message("이미 등록된 전화번호입니다.")
                            .build();
                }
            }

            // 취미 데이터의 유효성 검증
            if (request.getHobbies() != null && !request.getHobbies().isEmpty()) {
                for (HobbyRequest hobby : request.getHobbies()) {
                    // 카테고리 ID가 누락된 경우
                    if (hobby.getCategoryId() == null) {
                        return SignupResponse.builder()
                                .success(false)
                                .message("카테고리 정보가 누락되었습니다.")
                                .build();
                    }
                    
                    // 취미 ID가 누락된 경우
                    if (hobby.getHobbyId() == null) {
                        return SignupResponse.builder()
                                .success(false)
                                .message("취미 정보가 누락되었습니다.")
                                .build();
                    }
                    
                    // 카테고리 유효성 검증
                    if (!hobbyService.isValidCategory(hobby.getCategoryId())) {
                        return SignupResponse.builder()
                                .success(false)
                                .message("유효하지 않은 카테고리입니다: " + hobby.getCategoryId())
                                .build();
                    }
                    
                    // 취미 유효성 검증
                    if (!hobbyService.isValidHobby(hobby.getHobbyId())) {
                        return SignupResponse.builder()
                                .success(false)
                                .message("유효하지 않은 취미입니다: " + hobby.getHobbyId())
                                .build();
                    }
                    
                    // 취미가 해당 카테고리에 속하는지 검증
                    try {
                        boolean isValid = hobbyService.getHobbyMapper().isHobbyInCategory(hobby.getHobbyId(), hobby.getCategoryId());
                        if (!isValid) {
                            return SignupResponse.builder()
                                    .success(false)
                                    .message("선택한 취미가 해당 카테고리에 속하지 않습니다. 취미ID: " + hobby.getHobbyId() + ", 카테고리ID: " + hobby.getCategoryId())
                                    .build();
                        }
                    } catch (Exception e) {
                        log.error("취미-카테고리 관계 검증 중 오류 발생: {}", e.getMessage());
                        return SignupResponse.builder()
                                .success(false)
                                .message("취미 정보 검증 중 오류가 발생했습니다.")
                                .build();
                    }
                }
            }

            // 비밀번호 해싱
            String hashedPassword = passwordUtils.hashPassword(request.getPassword(), null).get("hashedPassword");

            // 현재 시간 설정
            LocalDateTime now = LocalDateTime.now();

            // 사용자 정보 생성
            User user = User.builder()
                    .email(request.getEmail())
                    .passwordHash(hashedPassword)
                    .name(request.getName())
                    .phoneNumber(request.getPhoneNumber())
                    .nickname(request.getNickname())
                    .bio(request.getBio())
                    .loginMethod(request.getLoginMethod() != null ? request.getLoginMethod() : "EMAIL")
                    .socialProvider(request.getSocialProvider() != null ? request.getSocialProvider() : "NONE")
                    .accountStatus("Active")
                    .authority("USER")
                    .signupDate(now)
                    .lastUpdateDate(now)
                    .lastLoginTime(null)
                    .loginFailedAttempts(0)
                    .loginIsLocked(false)
                    .build();

            // 사용자 등록
            int result = userMapper.insertUser(user);

            if (result > 0) {

                // 초기 도파민 수치(50)와 활동 포인트(0) 설정
                int initialDopamine = 50;
                int initialPoints = 0;
                userMapper.initializeUserActivity(request.getEmail(), initialDopamine, initialPoints);

                // 취미 정보가 있다면 등록
                if (request.getHobbies() != null && !request.getHobbies().isEmpty()) {
                    hobbyService.registerUserHobbies(request.getEmail(), request.getHobbies());
                }

                return SignupResponse.builder()
                        .success(true)
                        .email(request.getEmail())
                        .message("회원가입에 성공하였습니다.")
                        .initialDopamine(initialDopamine)
                        .initialPoints(initialPoints)
                        .build();
            } else {
                log.error("회원가입 실패 - 사용자 기본 정보 저장 실패, 이메일: {}, insertUser 결과: {}", request.getEmail(), result);
                return SignupResponse.builder()
                        .success(false)
                        .message("회원가입에 실패하였습니다.")
                        .build();
            }
        } catch (Exception e) {
            log.error("회원가입 중 오류 발생 - 이메일: {}", request.getEmail(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return SignupResponse.builder()
                    .success(false)
                    .message("회원가입 중 오류가 발생했습니다.")
                    .build();
        }
    }

    /**
     * 로그인
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 사용자 정보 조회
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            return LoginResponse.builder()
                    .success(false)
                    .message("잘못된 이메일 또는 비밀번호입니다.")
                    .build();
        }

        // 계정 상태 확인
        if ("Withdrawal".equals(user.getAccountStatus())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("탈퇴한 계정입니다.")
                    .build();
        } else if (!"Active".equals(user.getAccountStatus())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("현재 계정이 활성 상태가 아닙니다.")
                    .build();
        }

        // 계정 잠금 확인
        if (Boolean.TRUE.equals(user.getLoginIsLocked())) {
            return LoginResponse.builder()
                    .success(false)
                    .message("반복된 로그인 실패로 계정이 잠금되었습니다.")
                    .build();
        }

        // 비밀번호 검증
        boolean isPasswordValid = passwordUtils.verifyPassword(
                request.getPassword(),
                user.getPasswordHash(),
                null
        );

        if (!isPasswordValid) {
            // 로그인 실패 횟수 증가
            int failedAttempts = user.getLoginFailedAttempts() + 1;
            userMapper.updateFailedLoginAttempts(user.getEmail(), failedAttempts);

            // 최대 실패 횟수 초과 시 계정 잠금
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                userMapper.updateLoginLockStatus(user.getEmail(), true);
                return LoginResponse.builder()
                        .success(false)
                        .message("로그인 실패 횟수가 초과되어 계정이 잠금되었습니다.")
                        .build();
            }

            return LoginResponse.builder()
                    .success(false)
                    .message("잘못된 이메일 또는 비밀번호입니다.")
                    .build();
        }

        // 로그인 성공 → 실패 카운트 초기화 및 마지막 로그인 시간 업데이트
        userMapper.updateFailedLoginAttempts(user.getEmail(), 0);
        userMapper.updateLoginTime(user.getEmail());

        // 권한 설정
        String role = "ROLE_" + user.getAuthority().toUpperCase();

        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(
                user.getId().intValue(),
                user.getEmail(),
                Collections.singletonList(role)
        );

        return LoginResponse.builder()
                .success(true)
                .token(token)
                .email(user.getEmail())
                .nickname(user.getNickname())
                .message("로그인 성공")
                .build();
    }

    /**
     * 로그아웃
     */
    @Transactional
    public LogoutResponse logout(String token) {
        if (token == null || token.isEmpty()) {
            return LogoutResponse.builder()
                    .success(false)
                    .message("토큰이 제공되지 않았습니다.")
                    .build();
        }

        try {
            // Bearer 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token) || jwtTokenBlacklistService.isBlacklisted(token)) {
                return LogoutResponse.builder()
                        .success(false)
                        .message("유효하지 않은 토큰입니다.")
                        .build();
            }

            // 토큰 만료 시간 가져오기
            Date expiration = jwtTokenProvider.getExpirationDate(token);
            Long expiryMillis = expiration.getTime();

            // 블랙리스트 추가
            jwtTokenBlacklistService.addToBlacklist(token, expiryMillis);

            LocalDateTime expiryDateTime = LocalDateTime.ofInstant(
                    expiration.toInstant(), ZoneId.systemDefault());

            return LogoutResponse.builder()
                    .success(true)
                    .message("성공적으로 로그아웃되었습니다.")
                    .invalidatedUntil(expiryDateTime)
                    .build();
        } catch (Exception e) {
            return LogoutResponse.builder()
                    .success(false)
                    .message("로그아웃 중 오류 발생: " + e.getMessage())
                    .build();
        }
    }
}
