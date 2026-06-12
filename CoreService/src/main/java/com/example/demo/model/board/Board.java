package com.example.demo.model.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    private Long id;
    private String name;
    private String description;
    private String imagePath;
    private String hostEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status; // ACTIVE, ARCHIVED, DELETED
    
    // 추가 정보 (조인 결과)
    private String hostName;
    private Integer memberCount;
}
