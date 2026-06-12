package com.example.demo.dto.Market;
// 상품 요청 전용 DTO

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequestDto {
    @NotNull
    private Long productId; // 요청할 상품 ID
}