package org.example.springboot.dto.ai;

import lombok.Data;

@Data
public class AiProductRecommendation {
    private Long id;
    private String name;
    private String category;
    private Double price;
    private Integer stock;
    private Integer salesCount;
    private String reason;
    private Double score;
}