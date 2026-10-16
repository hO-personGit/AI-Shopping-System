package org.example.springboot.dto.ai;

import lombok.Data;

@Data
public class AiSalesAnalysisResponse {
    private String hotProductsAnalysis;
    private String stockWarning;
    private String replenishmentAdvice;
    private String salesTrendSummary;
    private String summary;
    private String source;
}