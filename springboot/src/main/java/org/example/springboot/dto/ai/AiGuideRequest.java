package org.example.springboot.dto.ai;

import lombok.Data;

@Data
public class AiGuideRequest {
    private String query;
    private Long userId;
    private Integer topK = 5;
}