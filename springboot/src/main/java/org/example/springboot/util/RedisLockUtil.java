package org.example.springboot.util;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分布式锁工具（Redis SETNX + 过期时间 + 唯一标识释放）。
 *
 * <p>用于订单支付防超卖等并发场景：
 * <ul>
 *   <li>加锁：SET key value NX EX ttl（原子操作，value 为唯一请求 ID）。</li>
 *   <li>解锁：Lua 脚本校验 value 后删除，避免误删他人锁。</li>
 *   <li>降级：Redis 不可用时自动降级为 JVM 本地锁（ReentrantLock），保证演示环境可运行。</li>
 * </ul>
 */
@Component
public class RedisLockUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisLockUtil.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /** 本地锁降级集合（进程内锁，仅单机降级用）。 */
    private final ConcurrentHashMap<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    /**
     * 尝试获取分布式锁。
     *
     * @param lockKey 锁键（如 "lock:order:pay:{productId}"）
     * @param ttlSeconds 锁过期时间（秒），防止死锁
     * @return 成功返回释放用的 token（requestId），失败返回 null
     */
    public String tryLock(String lockKey, long ttlSeconds) {
        try {
            String requestId = UUID.randomUUID().toString();
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, requestId, Duration.ofSeconds(ttlSeconds));
            if (Boolean.TRUE.equals(ok)) {
                return requestId;
            }
            return null;
        } catch (Exception e) {
            // Redis 不可用：降级为本地锁（单机演示环境）
            LOGGER.warn("Redis 不可用，降级为本地锁。lockKey={}, err={}", lockKey, e.getMessage());
            ReentrantLock local = localLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
            boolean got = false;
            try {
                got = local.tryLock(ttlSeconds, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            return got ? "LOCAL-" + UUID.randomUUID() : null;
        }
    }

    /**
     * 释放分布式锁（Lua 脚本保证原子性：仅当 value 匹配时才删除）。
     */
    public void unlock(String lockKey, String requestId) {
        if (lockKey == null || requestId == null) {
            return;
        }
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            org.springframework.data.redis.core.script.DefaultRedisScript<Long> redisScript =
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class);
            stringRedisTemplate.execute(redisScript, java.util.List.of(lockKey), requestId);
        } catch (Exception e) {
            LOGGER.warn("释放 Redis 锁失败，lockKey={}, err={}", lockKey, e.getMessage());
            ReentrantLock local = localLocks.get(lockKey);
            if (local != null && local.isHeldByCurrentThread()) {
                local.unlock();
            }
        }
    }
}
