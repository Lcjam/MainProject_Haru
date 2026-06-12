package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardResponse {
    private Long id;
    private String name;
    private String description;
    private String imagePath;
    private String hostEmail;
    private String hostName;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status;
}
