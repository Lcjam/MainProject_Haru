package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hobby {
    private Long hobbyId;
    private String hobbyName;
    private List<Category> categories; // 이 취미에 속한 카테고리 목록
}
