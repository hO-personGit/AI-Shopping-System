package org.example.springboot.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * HTTP 可观测指标。
 *
 * <p>指标清单（Prometheus 采集，路径 /actuator/prometheus）：
 * <ul>
 *   <li>{@code http_requests_total}：请求总数（Counter）</li>
 *   <li>{@code http_requests_status_total{method,status}}：按状态码分布</li>
 *   <li>{@code http_requests_path_total{path}}：按归一化路径分布</li>
 *   <li>{@code http_requests_duration_seconds}：请求耗时（Timer，含 P50/P95/P99）</li>
 * </ul>
 */
@Component
public class ApiMetrics {

    private final Counter requestCounter;
    private final Timer requestTimer;
    private final MeterRegistry meterRegistry;

    public ApiMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.requestCounter = Counter.builder("http.requests.total")
                .description("HTTP 请求总数")
                .register(meterRegistry);
        this.requestTimer = Timer.builder("http.requests.duration")
                .description("HTTP 请求耗时")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void record(String method, String path, int status, long costMs) {
        requestCounter.increment();
        requestTimer.record(Duration.ofMillis(costMs));
        meterRegistry.counter("http.requests.status",
                "method", method, "status", String.valueOf(status)).increment();
        meterRegistry.counter("http.requests.path", "path", path).increment();
    }
}
