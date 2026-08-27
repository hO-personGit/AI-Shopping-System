package org.example.springboot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.dto.ai.AiCopywritingRequest;
import org.example.springboot.dto.ai.AiGuideRequest;
import org.example.springboot.dto.ai.AiSalesAnalysisRequest;
import org.example.springboot.service.AiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @Resource
    private ObjectMapper objectMapper;

    @Value("${ai.service.base-url:http://localhost:8001}")
    private String aiServiceBaseUrl;

    /** SSE 流式代理使用的独立线程池，避免阻塞 Tomcat 请求线程。 */
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

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

    /**
     * AI 智能导购 SSE 流式代理：透传 FastAPI 的 /ai/guide/stream 事件流，
     * 前端可用 EventSource / fetch 实现打字机式输出。
     */
    @PostMapping(value = "/guide/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter smartGuideStream(@RequestBody AiGuideRequest request) {
        SseEmitter emitter = new SseEmitter(60_000L);
        sseExecutor.execute(() -> {
            try {
                String url = aiServiceBaseUrl + "/ai/guide/stream";
                String body = objectMapper.writeValueAsString(request);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<java.io.InputStream> response =
                        HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    emitter.send(SseEmitter.event().data("{\"error\":\"AI服务返回异常: " + response.statusCode() + "\"}"));
                    emitter.complete();
                    return;
                }
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (!line.startsWith("data:")) {
                            continue;
                        }
                        emitter.send(SseEmitter.event().data(line.substring(5).trim(), MediaType.TEXT_PLAIN));
                        if (line.contains("[DONE]")) {
                            break;
                        }
                    }
                }
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().data("{\"error\":\"" + ex.getMessage() + "\"}"));
                } catch (Exception ignored) {
                }
                emitter.completeWithError(ex);
            }
        });
        return emitter;
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
