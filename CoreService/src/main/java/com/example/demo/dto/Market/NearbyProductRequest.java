package com.example.demo.dto.Market;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NearbyProductRequest {
    private double latitude;
    private double longitude;
    private double distance;
}
