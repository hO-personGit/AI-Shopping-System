package org.example.springboot.dto.ai;

import lombok.Data;

@Data
public class AiCopywritingResponse {
    private String title;
    private String summary;
    private String detail;
    private String slogan;
    private String source;
}