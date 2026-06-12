package com.example.demo.service;

import com.example.demo.dto.profile.*;
import com.example.demo.dto.hobby.*;
import com.example.demo.dto.auth.*;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.model.UserHobby;
import com.example.demo.util.PasswordUtils;
import com.example.demo.util.TokenUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserMapper userMapper;
    private final HobbyService hobbyService;
    private final PasswordUtils passwordUtils;
    private final TokenUtils tokenUtils;
    private final ProfileImageService profileImageService;

    /**
     * 사용자 프로필 조회
     */
    public ProfileResponse getUserProfile(String email) {
        // 사용자 기본 정보 조회
        User user = userMapper.findByEmail(email);

        if (user == null) {
            return ProfileResponse.builder()
                    .success(false)
                    .message("존재하지 않는 사용자입니다.")
                    .build();
        }

        // 도파민 수치와 활동 포인트 조회
        Integer dopamine = userMapper.getUserDopamine(email);
        Integer points = userMapper.getUserPoints(email);

        // 사용자 취미 정보 조회
        List<UserHobby> userHobbies = hobbyService.getUserHobbies(email);
        List<ProfileResponse.HobbyInfo> hobbyInfoList = userHobbies.stream()
                .map(ProfileResponse.HobbyInfo::fromUserHobby)
                .collect(Collectors.toList());

        // 프로필 이미지 URL 생성
        String profileImageUrl = profileImageService.getProfileImageUrl(email);

        // 프로필 응답 구성
        return ProfileResponse.builder()
                .success(true)
                .message("프로필 조회 성공")
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(profileImageUrl)
                .bio(user.getBio())
                .loginMethod(user.getLoginMethod())
                .accountStatus(user.getAccountStatus())
                .signupDate(user.getSignupDate())
                .lastLoginTime(user.getLastLoginTime())
                .dopamine(dopamine) // 도파민 수치
                .points(points) // 활동 포인트 추가
                .hobbies(hobbyInfoList)
                .build();
    }

    /**
     * 토큰으로 사용자 프로필 조회
     */
    public ProfileResponse getUserProfileByToken(String token) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);

        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ProfileResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }

        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        return getUserProfile(email);
    }

    /**
     * 닉네임으로 사용자 프로필 조회 (공개 정보만)
     */
    public ProfileResponse getPublicProfile(String nickname) {
        // 닉네임으로 사용자 조회
        User user = userMapper.findByNickname(nickname);

        if (user == null) {
            return ProfileResponse.builder()
                    .success(false)
                    .message("존재하지 않는 사용자입니다.")
                    .build();
        }

        // 계정이 활성 상태인지 확인
        if (!"Active".equals(user.getAccountStatus())) {
            return ProfileResponse.builder()
                    .success(false)
                    .message("비활성화된 계정입니다.")
                    .build();
        }

        // 도파민 수치와 활동 포인트 조회
        Integer dopamine = userMapper.getUserDopamine(user.getEmail());
        Integer points = userMapper.getUserPoints(user.getEmail());

        // 취미 정보 조회
        List<UserHobby> userHobbies = hobbyService.getUserHobbies(user.getEmail());
        List<ProfileResponse.HobbyInfo> hobbyInfoList = userHobbies.stream()
                .map(ProfileResponse.HobbyInfo::fromUserHobby)
                .collect(Collectors.toList());

        // 프로필 이미지 URL 생성
        String profileImageUrl = profileImageService.getProfileImageUrl(user.getEmail());

        // 공개 프로필만 반환 (민감한 정보 제외)
        return ProfileResponse.builder()
                .success(true)
                .message("프로필 조회 성공")
                .nickname(user.getNickname())
                .profileImageUrl(profileImageUrl)
                .bio(user.getBio())
                .dopamine(dopamine) // 도파민 수치
                .points(points) // 활동 포인트 추가
                .hobbies(hobbyInfoList)
                .build();
    }

    /**
     * 사용자 프로필 업데이트
     */
    @Transactional
    public ProfileUpdateResponse updateProfile(String email, ProfileUpdateRequest request) {
        // 사용자 존재 여부 확인
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ProfileUpdateResponse.builder()
                    .success(false)
                    .message("존재하지 않는 사용자입니다.")
                    .build();
        }

        // 닉네임 중복 체크 (변경된 경우에만)
        if (StringUtils.hasText(request.getNickname()) && !request.getNickname().equals(user.getNickname())) {
            User existingUser = userMapper.findByNicknameExceptEmail(request.getNickname(), email);
            if (existingUser != null) {
                return ProfileUpdateResponse.builder()
                        .success(false)
                        .message("이미 사용 중인 닉네임입니다.")
                        .build();
            }
        }

        // 취미 데이터의 유효성 검증 (카테고리->취미 선택 방식)
        if (request.getHobbies() != null && !request.getHobbies().isEmpty()) {
            for (HobbyRequest hobby : request.getHobbies()) {
                // 카테고리 ID가 누락된 경우
                if (hobby.getCategoryId() == null) {
                    return ProfileUpdateResponse.builder()
                            .success(false)
                            .message("카테고리 정보가 누락되었습니다.")
                            .build();
                }

                // 취미 ID가 누락된 경우
                if (hobby.getHobbyId() == null) {
                    return ProfileUpdateResponse.builder()
                            .success(false)
                            .message("취미 정보가 누락되었습니다.")
                            .build();
                }

                // 취미가 해당 카테고리에 속하는지 검증
                try {
                    boolean isValid = hobbyService.getHobbyMapper().isHobbyInCategory(hobby.getHobbyId(),
                            hobby.getCategoryId());
                    if (!isValid) {
                        return ProfileUpdateResponse.builder()
                                .success(false)
                                .message("선택한 취미가 해당 카테고리에 속하지 않습니다. 취미ID: " + hobby.getHobbyId() + ", 카테고리ID: "
                                        + hobby.getCategoryId())
                                .build();
                    }
                } catch (Exception e) {
                  
                    return ProfileUpdateResponse.builder()
                            .success(false)
                            .message("취미 정보 검증 중 오류가 발생했습니다.")
                            .build();
                }
            }
        }

        // 프로필 정보 업데이트
        User updatedUser = User.builder()
                .email(email)
                .name(request.getName() != null ? request.getName() : user.getName())
                .nickname(request.getNickname() != null ? request.getNickname() : user.getNickname())
                .phoneNumber(user.getPhoneNumber()) // 전화번호는 변경 불가
                .bio(request.getBio() != null ? request.getBio() : user.getBio())
                .lastUpdateDate(LocalDateTime.now())
                .build();

        userMapper.updateUserProfile(updatedUser);

        // 취미 정보 업데이트 (요청에 포함된 경우)
        if (request.getHobbies() != null && !request.getHobbies().isEmpty()) {
            try {
                hobbyService.registerUserHobbies(email, request.getHobbies());
               
                      
            } catch (Exception e) {
          
                // 취미 등록 실패해도 프로필 업데이트는 성공으로 처리
            }
        }

        // 업데이트된 프로필 정보 조회
        ProfileResponse updatedProfile = getUserProfile(email);

        return ProfileUpdateResponse.builder()
                .success(true)
                .message("프로필이 성공적으로 업데이트되었습니다.")
                .updatedProfile(updatedProfile)
                .build();
    }

    /**
     * 토큰을 통한 사용자 프로필 업데이트
     */
    public ProfileUpdateResponse updateProfileByToken(String token, ProfileUpdateRequest request) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);

        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return ProfileUpdateResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }

        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        return updateProfile(email, request);
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public PasswordChangeResponse changePassword(String email, PasswordChangeRequest request) {
        // 사용자 존재 여부 확인 이메일 존재하는지 확인하고
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return PasswordChangeResponse.builder()
                    .success(false)
                    .message("존재하지 않는 사용자입니다.")
                    .build();
        }
        // 프론트에서 보낸 isToken이 true이니까
        if (request.getIsToken().equals("true")) {
            // 현재 비밀번호 확인 이곳을 실행합니다다
            boolean isCurrentPasswordValid = passwordUtils.verifyPassword(
                    request.getCurrentPassword(),
                    user.getPasswordHash(),
                    null);

            if (!isCurrentPasswordValid) {
                return PasswordChangeResponse.builder()
                        .success(false)
                        .message("현재 비밀번호가 일치하지 않습니다.")
                        .build();
            }
        } else { // if문이 성공했으니 여기는 건너뜁니다다
            String verifyPhoneNumber = userMapper.findByEmail(request.getEmail()).getPhoneNumber().trim();
            System.out.println(
                    "VerifyPhoneNumber: " + verifyPhoneNumber + "\n" + "getPhoneNumber:" + request.getPhoneNumber());
            if (!verifyPhoneNumber.equals(request.getPhoneNumber())) {
                return PasswordChangeResponse.builder()
                        .success(false)
                        .message("전화번호가 일치하지 않습니다.")
                        .build();
            }
        }

        // 새 비밀번호와 확인 비밀번호 일치 확인
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return PasswordChangeResponse.builder()
                    .success(false)
                    .message("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.")
                    .build();
        }

        // 비밀번호 변경
        String newPasswordHash = passwordUtils.hashPassword(request.getNewPassword(), null).get("hashedPassword");
        userMapper.updateUserPasswordHash(email, newPasswordHash);
        // 여기까지 오면 성공입니다다
        return PasswordChangeResponse.builder()
                .success(true)
                .message("비밀번호가 성공적으로 변경되었습니다.")
                .build();
        // 반환 값으로는 success boolean과 message String으로 반환됩니다다
    }

    /**
     * 토큰을 통한 비밀번호 변경
     */
    public PasswordChangeResponse changePasswordByToken(String token, PasswordChangeRequest request) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);
        // 여기서 토큰 검증하고고
        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return PasswordChangeResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }
        // 토큰에서 이메일 추출
        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        // 이제서야 어제 제가 수정했던 메서드를 호출합니다
        // 실제 비밀번호 변경이 일어나는 메서드입니다다
        return changePassword(email, request);
    }

    /**
     * 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageInfo(String email) {
        // 사용자 기본 정보 조회
        User user = userMapper.findByEmail(email);

        if (user == null) {
            return MyPageResponse.builder()
                    .success(false)
                    .message("존재하지 않는 사용자입니다.")
                    .build();
        }

        // 도파민 수치와 활동 포인트 조회
        Integer dopamine = userMapper.getUserDopamine(email);
        Integer points = userMapper.getUserPoints(email);

        // 프로필 이미지 URL 생성
        String profileImageUrl = profileImageService.getProfileImageUrl(email);

        // 마이페이지 응답 구성 (제한된 정보만 포함)
        return MyPageResponse.builder()
                .success(true)
                .message("마이페이지 정보 조회 성공")
                .name(user.getName())
                .nickname(user.getNickname())
                .signupDate(user.getSignupDate())
                .lastLoginTime(user.getLastLoginTime())
                .profileImageUrl(profileImageUrl)
                .accountStatus(user.getAccountStatus())
                .dopamine(dopamine) // 도파민 수치
                .points(points) // 활동 포인트 추가
                .build();
    }

    /**
     * 토큰으로 마이페이지 정보 조회
     */
    public MyPageResponse getMyPageInfoByToken(String token) {
        String tokenWithoutBearer = tokenUtils.extractTokenWithoutBearer(token);

        if (!tokenUtils.isTokenValid(tokenWithoutBearer)) {
            return MyPageResponse.builder()
                    .success(false)
                    .message("유효하지 않은 인증 토큰입니다.")
                    .build();
        }

        String email = tokenUtils.getEmailFromToken(tokenWithoutBearer);
        return getMyPageInfo(email);
    }
}
