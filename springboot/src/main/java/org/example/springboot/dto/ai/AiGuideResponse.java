package org.example.springboot.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiGuideResponse {
    private String answer;
    private List<AiProductRecommendation> recommendations;
    private String source;
}