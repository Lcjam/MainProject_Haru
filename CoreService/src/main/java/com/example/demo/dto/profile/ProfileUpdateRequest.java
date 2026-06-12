package com.example.demo.dto.profile;

import com.example.demo.dto.hobby.HobbyRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    // 수정 가능한 필드들로 제한
    private String name;         // 이름 변경 가능
    private String nickname;     // 닉네임 변경 가능
    private String bio;          // 소개글 변경 가능
    private List<HobbyRequest> hobbies; // 취미/카테고리 변경 가능
    
    // 이메일과 전화번호는 변경 불가능하므로 필드에서 제외됨
}
