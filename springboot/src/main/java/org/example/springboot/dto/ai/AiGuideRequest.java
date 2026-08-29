package org.example.springboot.dto.ai;

import lombok.Data;

@Data
public class AiGuideRequest {
    private String query;
    private Long userId;
    /** 会话 ID：用于 AI 多轮对话上下文管理，前端每次对话传入同一 sessionId 可连续追问 */
    private String sessionId;
    private Integer topK = 5;
}
