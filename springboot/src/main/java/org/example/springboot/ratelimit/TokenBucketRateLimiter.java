package org.example.springboot.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 进程内令牌桶限流器（无 Redis 依赖，适合单机部署与演示环境）。
 *
 * <p>核心思想：
 * <ul>
 *   <li>每个 key 一个桶，桶容量 {@code capacity} 决定瞬时突发上限；</li>
 *   <li>按 {@code permitsPerSecond} 速率匀速补充令牌，取桶内不足则拒绝；</li>
 *   <li>空闲桶定时清理，避免 key 无限膨胀（内存安全）。</li>
 * </ul>
 *
 * <p>生产环境可替换为 Redis + Lua 脚本实现分布式限流，本实现保持接口不变。
 */
@Component
public class TokenBucketRateLimiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBucketRateLimiter.class);

    /** 空闲桶清理周期（秒）。 */
    private static final long CLEANUP_INTERVAL_SECONDS = 60;
    /** 桶空闲超过该时长即清理（秒）。 */
    private static final long BUCKET_IDLE_SECONDS = 300;

    private static final class Bucket {
        final double capacity;
        double tokens;
        long lastRefillNanos;

        Bucket(double capacity) {
            this.capacity = capacity;
            this.tokens = capacity; // 初始满桶，允许突发
            this.lastRefillNanos = System.nanoTime();
        }
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rate-limiter-cleaner");
        t.setDaemon(true);
        return t;
    });

    public TokenBucketRateLimiter() {
        cleaner.scheduleAtFixedRate(this::cleanup, CLEANUP_INTERVAL_SECONDS, CLEANUP_INTERVAL_SECONDS, TimeUnit.SECONDS);
        LOGGER.info("TokenBucketRateLimiter 已启动，空闲桶 {}s 清理一次", CLEANUP_INTERVAL_SECONDS);
    }

    /**
     * 尝试获取 1 个令牌。
     *
     * @param key              限流维度（接口 / 用户 / IP 等）
     * @param permitsPerSecond 令牌补充速率
     * @param capacity         桶容量
     * @return true 放行 / false 拒绝
     */
    public boolean tryAcquire(String key, double permitsPerSecond, double capacity) {
        long now = System.nanoTime();
        Bucket bucket = buckets.compute(key, (k, b) -> {
            if (b == null) {
                return new Bucket(capacity);
            }
            refill(b, permitsPerSecond, now);
            return b;
        });
        synchronized (bucket) {
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    private void refill(Bucket bucket, double permitsPerSecond, long now) {
        double elapsedSeconds = (now - bucket.lastRefillNanos) / 1_000_000_000.0;
        bucket.tokens = Math.min(bucket.capacity, bucket.tokens + elapsedSeconds * permitsPerSecond);
        bucket.lastRefillNanos = now;
    }

    private void cleanup() {
        long cutoffNanos = System.nanoTime() - BUCKET_IDLE_SECONDS * 1_000_000_000L;
        buckets.entrySet().removeIf(e -> {
            Bucket b = e.getValue();
            synchronized (b) {
                // 桶已满且长时间未使用 → 可回收
                return b.lastRefillNanos < cutoffNanos && b.tokens >= b.capacity;
            }
        });
    }

    /** 当前活跃桶数量（供监控/测试）。 */
    public int size() {
        return buckets.size();
    }
}
