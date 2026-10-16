package org.example.springboot.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 令牌桶限流器单元测试：容量突发、速率补充、超限拒绝。
 */
class TokenBucketRateLimiterTest {

    @Test
    void 容量内突发全部放行() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        // 容量 10，初始满桶 → 连续 10 次全部放行
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.tryAcquire("k1", 5, 10), "第 " + (i + 1) + " 次应放行");
        }
        // 第 11 次令牌耗尽被拒
        assertFalse(limiter.tryAcquire("k1", 5, 10), "超容量应拒绝");
    }

    @Test
    void 速率不足时被拒绝() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        // 速率 1/s，容量 1 → 第二次立即请求被拒
        assertTrue(limiter.tryAcquire("k2", 1, 1));
        assertFalse(limiter.tryAcquire("k2", 1, 1), "同秒第二次应被限流");
    }

    @Test
    void 不同key相互隔离() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        assertTrue(limiter.tryAcquire("a", 1, 1));
        assertFalse(limiter.tryAcquire("a", 1, 1));
        // key=b 独立桶，仍可放行
        assertTrue(limiter.tryAcquire("b", 1, 1));
    }

    @Test
    void 长时间后令牌补充可再次放行() throws InterruptedException {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter();
        assertTrue(limiter.tryAcquire("k3", 10, 1));
        assertFalse(limiter.tryAcquire("k3", 10, 1));
        Thread.sleep(300); // 补充 10/s * 0.3s ≈ 3 个令牌
        assertTrue(limiter.tryAcquire("k3", 10, 1), "补充后应可放行");
    }
}
