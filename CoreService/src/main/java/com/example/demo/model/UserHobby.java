package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserHobby {
    private Long id;
    private String email;
    private Long hobbyId;
    private Long categoryId;
    
    // 조회 시 이름도 같이 불러오기 위한 필드
    private String hobbyName;
    private String categoryName;
}
