package com.example.demo.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProfileImage {
    private Long id;
    private String username;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
