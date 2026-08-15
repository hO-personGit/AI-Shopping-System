package org.example.springboot.controller;

import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.dto.ai.AiCopywritingRequest;
import org.example.springboot.dto.ai.AiGuideRequest;
import org.example.springboot.dto.ai.AiSalesAnalysisRequest;
import org.example.springboot.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @PostMapping("/guide")
    public Result<?> smartGuide(@RequestBody AiGuideRequest request) {
        try {
            if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                return Result.error("400", "Please input shopping requirement");
            }
            return Result.success(aiService.smartGuide(request));
        } catch (Exception ex) {
            return Result.error("500", ex.getMessage());
        }
    }

    @PostMapping("/copywriting")
    public Result<?> generateCopywriting(@RequestBody AiCopywritingRequest request) {
        try {
            return Result.success(aiService.generateCopywriting(request));
        } catch (Exception ex) {
            return Result.error("500", ex.getMessage());
        }
    }

    @PostMapping("/sales-analysis")
    public Result<?> analyzeSales(@RequestBody(required = false) AiSalesAnalysisRequest request) {
        try {
            return Result.success(aiService.analyzeSales(request));
        } catch (Exception ex) {
            return Result.error("500", ex.getMessage());
        }
    }
}