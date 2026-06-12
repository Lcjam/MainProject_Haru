package com.example.demo.model.Market;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    private Long id;
    private String productCode;
    private String title;
    private String description;
    private int price;
    private String email; // FK (Users 테이블)
    private Long categoryId; // FK (Categories 테이블)
    private Long hobbyId;  // FK (Hobbies 테이블)
    private String transactionType;
    private String registrationType;
    private int maxParticipants;  // 모집인원
    private int currentParticipants; // 현재 모집된 인원
    private boolean isVisible; // 모집 가능 여부
    private String days; // 추가
    private LocalDateTime startDate;  // 일정 시작일
    private LocalDateTime endDate;  // 일정 종료일
    private Double latitude; // 거래 장소 위도 (대면 거래일 경우 필수)
    private Double longitude; // 거래 장소 경도 (대면 거래일 경우 필수)
    private String meetingPlace; // 거래 장소명 (대면 거래일 경우 필수)
    private String address; // 거래 장소 주소 (선택적)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Join 한 유저 정보
    private String nickname;
    private String bio;
    private int dopamine;

    // Join 한 이미지 정보
    private List<String> imagePaths;  // 이미지 리스트
    private String thumbnailPath;  // 대표 이미지
}
