package org.example.springboot.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.metrics.ApiMetrics;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路 TraceId 过滤器。
 *
 * <p>职责：
 * <ul>
 *   <li>从请求头 {@code X-Trace-Id} 读取上游 TraceId，没有则生成新的；</li>
 *   <li>写入 SLF4J MDC（日志统一输出 traceId），并回写响应头；</li>
 *   <li>记录 HTTP 请求指标（QPS / 耗时 / 状态码分布），供 Prometheus 采集。</li>
 * </ul>
 *
 * <p>配合 {@code AiRestTemplateConfig} 与 AI 微服务 middleware，实现
 * 「前端 → 后端 → AI 微服务 / MQ 消费」全链路日志串联。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    private final ApiMetrics apiMetrics;

    public TraceIdFilter(ApiMetrics apiMetrics) {
        this.apiMetrics = apiMetrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank() || traceId.length() > 64) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long costMs = System.currentTimeMillis() - start;
            apiMetrics.record(request.getMethod(), sanitizePath(request.getRequestURI()),
                    response.getStatus(), costMs);
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    /** 生成 16 位十六进制 TraceId（与 AI 微服务对齐） */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /** 指标路径归一化：避免 userId/orderId 等动态段打爆标签基数 */
    private String sanitizePath(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        return uri.replaceAll("/\\d+", "/{id}");
    }
}
