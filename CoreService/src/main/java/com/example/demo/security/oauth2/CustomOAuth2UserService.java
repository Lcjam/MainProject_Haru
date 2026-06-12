package com.example.demo.security.oauth2;

import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (Exception e) {
            log.error("OAuth2 인증 처리 중 오류 발생: {}", e.getMessage());
            throw new InternalAuthenticationServiceException(e.getMessage(), e);
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        // OAuth2 사용자 정보 추출
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        if (!StringUtils.hasText(oAuth2UserInfo.getEmail())) {
            throw new RuntimeException("소셜 계정에서 이메일을 찾을 수 없습니다.");
        }

        // 기존 사용자인지 확인
        User user = userMapper.findByEmail(oAuth2UserInfo.getEmail());
        
        if (user == null) {
            // 신규 사용자일 경우 회원가입 처리
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo);
        } else {
            // 기존 사용자는 소셜 정보 업데이트
            user = updateExistingUser(user, oAuth2UserInfo);
        }

        return CustomOAuth2User.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo) {
        String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId().toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        
        // 닉네임 생성 (중복 방지를 위해 랜덤 문자열 추가)
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String nickname = oAuth2UserInfo.getName() + "_" + uniqueSuffix;
        
        // 새 사용자 정보 생성
        User user = User.builder()
                .email(oAuth2UserInfo.getEmail())
                .name(oAuth2UserInfo.getName())
                .nickname(nickname)
                .passwordHash(UUID.randomUUID().toString()) // 소셜 로그인은 비밀번호가 없으므로 랜덤하게 생성
                .profileImagePath(oAuth2UserInfo.getImageUrl())
                .bio("") // 기본 자기소개는 빈 문자열
                .loginMethod("SOCIAL")
                .socialProvider(registrationId)
                .accountStatus("Active")
                .authority("USER")
                .signupDate(now)
                .lastUpdateDate(now)
                .lastLoginTime(now)
                .loginFailedAttempts(0)
                .loginIsLocked(false)
                .build();
        
        userMapper.insertUser(user);
        
        // 초기 도파민 수치와 활동 포인트 설정
        userMapper.initializeUserActivity(user.getEmail(), 50, 0);
        
        return user;
    }

    private User updateExistingUser(User user, OAuth2UserInfo oAuth2UserInfo) {
        // 필요한 정보 업데이트
        if (StringUtils.hasText(oAuth2UserInfo.getName()) && !oAuth2UserInfo.getName().equals(user.getName())) {
            user.setName(oAuth2UserInfo.getName());
        }
        
        if (StringUtils.hasText(oAuth2UserInfo.getImageUrl()) && 
            (user.getProfileImagePath() == null || !oAuth2UserInfo.getImageUrl().equals(user.getProfileImagePath()))) {
            user.setProfileImagePath(oAuth2UserInfo.getImageUrl());
        }
        
        // 소셜 로그인 시간 업데이트
        userMapper.updateLoginTime(user.getEmail());
        
        return user;
    }

    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("naver")) {
            return new NaverOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("kakao")) {
            return new KakaoOAuth2UserInfo(attributes);
        } else {
            throw new RuntimeException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }
    }
}
