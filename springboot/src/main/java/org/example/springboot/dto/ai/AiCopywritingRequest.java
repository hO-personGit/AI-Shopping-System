package org.example.springboot.dto.ai;

import lombok.Data;

@Data
public class AiCopywritingRequest {
    private String name;
    private String category;
    private Double price;
    private Integer stock;
    private String placeOfOrigin;
    private String description;
    private String sellingPoints;
}