package com.example.demo.dto.Market;

import lombok.*;

/**
 * 사용자가 위치 정보를 업데이트할 때 요청 데이터를 받는 DTO
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LocationRequest {
    private String locationName; // 지역 이름
    private Double latitude;  // 위도
    private Double longitude; // 경도
}
