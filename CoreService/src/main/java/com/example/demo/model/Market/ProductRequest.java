package com.example.demo.model.Market;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    private Long id;
    private Long productId;
    private String requesterEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String status; // 대기, 진행중, 완료
    private String approvalStatus; // 승인, 미승인
}
