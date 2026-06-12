package com.example.demo.model.Market;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {
    private Long id;
    private Long productId;
    private String imagePath;

    @Builder.Default
    private Boolean isThumbnail = false;  // 기본값 false 적용

    private LocalDateTime createdAt;
}
