package com.example.demo.dto.hobby;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HobbyRequest {
    private Long hobbyId;
    private Long categoryId;
}
