package com.example.demo.model.Market;

import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 사용자 위치 정보를 저장하는 모델 (MyBatis 기반)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLocation {
    private Long id;  // 기본 키 (PK, AUTO_INCREMENT)
    private String email; // FK (Users 테이블 참조)
    private String locationName; // 지역 이름 (예: "서울 강남구")
    private double latitude;  // 위도
    private double longitude; // 경도
    private LocalDateTime recordedAt; // 마지막 업데이트 시간
}