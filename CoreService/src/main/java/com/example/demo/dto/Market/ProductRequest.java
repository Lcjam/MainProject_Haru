package com.example.demo.dto.Market;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {
    // private Long productId; // 상품 요청에만 필요

    @NotBlank
    private String title;
    @NotBlank
    private String description;
    @NotNull
    private Integer price;
    @NotNull
    private Long categoryId;
    private Long hobbyId; // 취미 ID 추가
    @NotBlank
    private String transactionType;
    @NotBlank
    private String registrationType;

    @Builder.Default
    @Min(1) // 최소 1명 이상
    private int maxParticipants = 1; // 모집 인원 추가

    private LocalDateTime startDate; // 일정 시작일 추가
    private LocalDateTime endDate; // 일정 종료일 추가
    private String meetingPlace;

    private List<String> days;

    private Double latitude;
    private Double longitude;
    private String address;

    // 이미지 URL 리스트
    private List<String> imagePaths;
}