package org.example.springboot.ratelimit;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 熔断器注册表：按名称持有 {@link CircuitBreakerState} 实例，同名熔断器共享状态。
 */
@Component
public class CircuitBreakerRegistry {

    private final ConcurrentHashMap<String, CircuitBreakerState> states = new ConcurrentHashMap<>();

    /**
     * 获取（或创建）指定名称的熔断状态。
     */
    public CircuitBreakerState get(String name, int failureThreshold, long windowSeconds,
                                   long openSeconds, double failureRateThreshold) {
        return states.computeIfAbsent(name, k -> new CircuitBreakerState(
                name, failureThreshold, windowSeconds, openSeconds, failureRateThreshold));
    }

    /** 当前注册的熔断器数量（供监控/测试）。 */
    public int size() {
        return states.size();
    }
}
