package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Location {
    private Long locationId;
    private Integer chatroomId;
    private String email;
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 추가 필드
    private String userNickname; // users 테이블과 JOIN 시 사용
} 