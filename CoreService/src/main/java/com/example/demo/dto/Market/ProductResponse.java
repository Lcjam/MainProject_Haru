package com.example.demo.dto.Market;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String productCode;
    private String title;
    private String description;
    private int price;
    private String email;
    private Long categoryId;
    private Long hobbyId;  // 취미 ID 추가
    private String transactionType;
    private String registrationType;
    private int maxParticipants;  // 모집인원 추가
    private int currentParticipants; // 추가
    private boolean isVisible;

    private List<String> days;  // 요일 정보 (월,화,수,목,금,토,일)

    private LocalDateTime startDate;  // 일정 시작일 추가
    private LocalDateTime endDate;  // 일정 종료일 추가
    private Double latitude; // 거래 장소 위도
    private Double longitude; // 거래 장소 경도
    private String meetingPlace; // 거래 장소명
    private String address; // 거래 장소 주소

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(); // Null 방지

    // 응답 시 이미지 리스트 포함
    private List<String> imagePaths;

    // 대표 이미지 추가
    private String thumbnailPath;

    // 유저 정보 추가
    private String nickname;
    private String bio;
    private int dopamine;


}
