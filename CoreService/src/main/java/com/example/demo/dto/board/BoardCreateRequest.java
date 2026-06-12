package com.example.demo.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCreateRequest {
    @NotBlank(message = "게시판 이름을 입력해주세요")
    @Size(min = 2, max = 100, message = "게시판 이름은 2~100자 사이여야 합니다")
    private String name;
    
    @Size(max = 500, message = "게시판 설명은 최대 500자까지 입력 가능합니다")
    private String description;
    
    private String imagePath;
}
