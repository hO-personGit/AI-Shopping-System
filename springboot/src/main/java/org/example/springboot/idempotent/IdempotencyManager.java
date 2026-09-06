package org.example.springboot.idempotent;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 进程内幂等结果管理器：缓存「幂等键 → 首次响应对象」。
 * 重复请求命中缓存直接返回首次结果，避免重复建单/扣款。
 *
 * <p>直接缓存返回对象（而非 JSON 字符串），保证 AOP 切面返回类型与方法签名一致。
 */
@Component
public class IdempotencyManager {

    /** 幂等键 → 首次响应对象。 */
    private final Cache<String, Object> results = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    /** 尝试获取已有结果（不存在返回 null）。 */
    public Object get(String key) {
        return results.getIfPresent(key);
    }

    /** 缓存首次执行结果。 */
    public void put(String key, Object result) {
        results.put(key, result);
    }
}
