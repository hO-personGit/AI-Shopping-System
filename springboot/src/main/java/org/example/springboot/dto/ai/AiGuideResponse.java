package org.example.springboot.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class AiGuideResponse {
    private String answer;
    private List<AiProductRecommendation> recommendations;
    private String source;
    /** AI Agent 实际调用的工具列表（如 search_products / query_stock） */
    private List<String> toolCalls;
    /** 是否命中问答缓存 */
    private Boolean cached;
}
