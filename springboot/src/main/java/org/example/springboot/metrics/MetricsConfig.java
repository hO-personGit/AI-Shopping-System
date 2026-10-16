package org.example.springboot.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.example.springboot.service.ProductCacheService;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存可观测指标：从 Caffeine {@code recordStats()} 暴露命中率等统计。
 *
 * <p>指标清单（Prometheus 采集）：
 * <ul>
 *   <li>{@code cache_l1_hit_rate}：一级缓存命中率</li>
 *   <li>{@code cache_l1_hit_count} / {@code cache_l1_miss_count}：命中/未命中次数</li>
 * </ul>
 */
@Configuration
public class MetricsConfig {

    public MetricsConfig(MeterRegistry meterRegistry, ProductCacheService productCacheService) {
        Gauge.builder("cache.l1.hit.rate", productCacheService,
                        svc -> svc.getCacheStats().hitRate())
                .description("一级缓存命中率")
                .register(meterRegistry);
        Gauge.builder("cache.l1.hit.count", productCacheService,
                        svc -> svc.getCacheStats().hitCount())
                .description("一级缓存命中次数")
                .register(meterRegistry);
        Gauge.builder("cache.l1.miss.count", productCacheService,
                        svc -> svc.getCacheStats().missCount())
                .description("一级缓存未命中次数")
                .register(meterRegistry);
    }
}
