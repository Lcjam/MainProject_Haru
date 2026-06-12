package com.example.demo.mapper;

import com.example.demo.model.User;
import com.example.demo.model.UserAccountInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserMapper {

    // 사용자 등록
    int insertUser(User user);

    // 사용자 계정 정보 추가
    int insertUserAccountInfo(UserAccountInfo userAccountInfo);

    // 이메일로 사용자 찾기
    User findByEmail(String email);

    // 전화번호로 사용자 찾기
    User findByPhoneNumber(String phoneNumber);

    // 닉네임으로 사용자 찾기
    User findByNickname(String nickname);

    // 로그인 실패 횟수 업데이트
    int updateFailedLoginAttempts(@Param("email") String email, @Param("attempts") Integer attempts);

    // 로그인 잠금 상태 업데이트
    int updateLoginLockStatus(@Param("email") String email, @Param("isLocked") Boolean isLocked);

    // 로그인 시간 업데이트
    int updateLoginTime(@Param("email") String email);

    // 비밀번호 해시 업데이트
    int updateUserPasswordHash(@Param("email") String email, @Param("newPasswordHash") String newPasswordHash);

    // 계정 상태 업데이트
    int updateAccountStatus(@Param("email") String email, @Param("accountStatus") String accountStatus);

    // 사용자 프로필 업데이트
    int updateUserProfile(User user);

    // 이메일 제외한 닉네임 중복 확인
    User findByNicknameExceptEmail(@Param("nickname") String nickname, @Param("email") String email);

    // 회원 탈퇴 (소프트 삭제) - 계정 상태 변경
    int withdrawUser(@Param("email") String email, @Param("withdrawalDate") java.time.LocalDateTime withdrawalDate);

    // 계정 정보 테이블의 계정 상태 업데이트
    int updateUserAccountInfoStatus(@Param("email") String email, @Param("accountStatus") String accountStatus);

    // 회원 탈퇴 시 사용자 정보 익명화
    int anonymizeUserData(
            @Param("email") String email,
            @Param("nickname") String newNickname,
            @Param("phoneNumber") String maskedPhoneNumber,
            @Param("bio") String newBio,
            @Param("withdrawalDate") java.time.LocalDateTime withdrawalDate
    );

    // 프로필 이미지 경로 업데이트
    int updateProfileImagePath(@Param("email") String email, @Param("profileImagePath") String profileImagePath);

    /**
     * 사용자의 도파민 수치 조회
     */
    Integer getUserDopamine(@Param("email") String email);

    /**
     * 사용자의 활동 포인트 조회
     */
    Integer getUserPoints(@Param("email") String email);

    /**
     * 사용자의 초기 도파민 수치와 활동 포인트 설정
     */
    int initializeUserActivity(@Param("email") String email,
                               @Param("dopamineValue") int dopamineValue,
                               @Param("pointsValue") int pointsValue);

    // 도파민 조회 (추가된 기능)
    // 기존 브랜치에서 빠져있던 도파민 조회 기능 유지
    // (이전 버전에서는 주석 처리되어 있었음)
    Integer getDopamineByEmail(@Param("email") String email);
}
