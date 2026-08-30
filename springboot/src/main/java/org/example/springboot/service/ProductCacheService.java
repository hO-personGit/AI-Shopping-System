package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Category;
import org.example.springboot.entity.Product;
import org.example.springboot.entity.User;
import org.example.springboot.mapper.CategoryMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.util.ProductBloomFilter;
import org.example.springboot.util.RedisLockUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商品缓存服务：针对「缓存穿透 / 击穿 / 雪崩」三大高并发缓存问题提供防护。
 *
 * <p>设计（面试/架构要点）：
 * <ul>
 *   <li><b>穿透</b>：请求不存在的数据 → 布隆过滤器（BitMapBloomFilter）先拦截，确定不存在的 key 直接短路，不打库；
 *       同时为空结果缓存短 TTL 空标记，二次拦截。</li>
 *   <li><b>击穿</b>：热点 key 过期瞬间大量并发打库 → 互斥锁（Redis 分布式锁 / JVM 本地锁）串行化重建，双检缓存。</li>
 *   <li><b>雪崩</b>：大量 key 同一时刻过期 → 缓存 TTL 叠加随机抖动（jitter），错峰过期。</li>
 * </ul>
 */
@Service
public class ProductCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCacheService.class);

    private static final String L2_KEY_PREFIX = "cache:product:detail:";
    private static final String LOCK_KEY_PREFIX = "lock:cache:product:detail:";
    private static final String NULL_MARKER = "{\"__null__\":true}";

    @Resource
    private ProductMapper productMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private CategoryMapper categoryMapper;
    @Resource
    private RedisLockUtil redisLockUtil;
    @Resource
    private ObjectMapper objectMapper;
    @Autowired(required = false)
    private StringRedisTemplate stringRedisTemplate;

    @Value("${cache.protection.enabled:true}")
    private boolean enabled;

    /** 布隆过滤器容量（商品总量级） */
    @Value("${cache.protection.bloom-size:20000}")
    private int bloomSize;

    /** 本地一级缓存（Caffeine）容量 */
    @Value("${cache.protection.l1-size:1000}")
    private int l1Size;

    /** 本地一级缓存 TTL（秒） */
    @Value("${cache.protection.l1-ttl-seconds:600}")
    private long l1TtlSeconds;

    /** Redis 二级缓存 TTL 基准（秒） */
    @Value("${cache.protection.l2-ttl-seconds:1800}")
    private long l2TtlSeconds;

    /** 缓存空值 TTL（秒），穿透兜底 */
    @Value("${cache.protection.null-ttl-seconds:30}")
    private long nullTtlSeconds;

    /** TTL 随机抖动上限（秒），防雪崩 */
    @Value("${cache.protection.ttl-jitter-seconds:120}")
    private long ttlJitterSeconds;

    /** 是否启用 Redis 二级缓存 */
    @Value("${cache.protection.l2-enabled:false}")
    private boolean l2Enabled;

    /** 本地锁重建互斥锁的本地实现（击穿防护降级） */
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.locks.ReentrantLock> localLocks =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 布隆过滤器：商品 ID 存在性快速判断 */
    private ProductBloomFilter bloomFilter;

    /** Caffeine 本地一级缓存 */
    private com.github.benmanes.caffeine.cache.Cache<String, Result<?>> l1Cache;

    @PostConstruct
    public void init() {
        // 按容量公式分配：m = -n·ln(p)/(ln2)²，误判率 0.01，避免 hutool 大容量 OOM
        bloomFilter = new ProductBloomFilter(bloomSize, 0.01);
        l1Cache = com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                .maximumSize(l1Size)
                .expireAfterWrite(Duration.ofSeconds(l1TtlSeconds))
                .recordStats()
                .build();
        if (enabled) {
            warmBloomFilter();
        }
        LOGGER.info("ProductCacheService 初始化完成：l1Size={} bloomSize={} l2Enabled={}",
                l1Size, bloomSize, l2Enabled);
    }

    /** 启动预热布隆过滤器：加载全部商品 ID，避免冷启动误判穿透 */
    private void warmBloomFilter() {
        try {
            List<Long> ids = productMapper.selectList(
                            new LambdaQueryWrapper<Product>().select(Product::getId))
                    .stream().map(Product::getId).toList();
            ids.forEach(id -> bloomFilter.add(String.valueOf(id)));
            LOGGER.info("布隆过滤器预热完成，加载商品 ID 数量：{}", ids.size());
        } catch (Exception e) {
            LOGGER.warn("布隆过滤器预热失败（数据库暂不可用），将按需回填：{}", e.getMessage());
        }
    }

    /**
     * 带「穿透/击穿/雪崩」防护的商品详情读取。
     *
     * @param productId 商品 ID
     * @return Result（未找到返回 error）
     */
    public Result<?> getProductDetail(Long productId) {
        String l1Key = L2_KEY_PREFIX + productId;
        String l2Key = L2_KEY_PREFIX + productId;

        // ---------- 一、穿透防护：布隆过滤器快速拦截确定不存在的 key ----------
        if (enabled && bloomFilter != null && !bloomFilter.contains(String.valueOf(productId))) {
            LOGGER.debug("布隆过滤器拦截不存在的商品 productId={}", productId);
            return Result.error("-1", "未找到商品");
        }

        // ---------- 二、一级缓存（Caffeine） ----------
        Result<?> cached = l1Cache.getIfPresent(l1Key);
        if (cached != null) {
            return cached;
        }
        // 空标记命中 → 直接返回不存在（穿透兜底）
        if (l1Cache.getIfPresent(NULL_MARKER + productId) != null) {
            return Result.error("-1", "未找到商品");
        }

        // ---------- 三、二级缓存（Redis） ----------
        if (l2Enabled && stringRedisTemplate != null) {
            Result<?> l2 = readFromRedis(l2Key);
            if (l2 != null) {
                l1Cache.put(l1Key, l2);
                return l2;
            }
        }

        // ---------- 四、击穿防护：互斥锁 + 双检，串行化重建 ----------
        String lockKey = LOCK_KEY_PREFIX + productId;
        String requestId = redisLockUtil.tryLock(lockKey, 10);
        if (requestId == null) {
            // 未拿到分布式锁 → 用本地锁再试一次（降级），仍失败则短暂等待重试
            java.util.concurrent.locks.ReentrantLock local =
                    localLocks.computeIfAbsent(lockKey, k -> new java.util.concurrent.locks.ReentrantLock());
            local.lock();
            try {
                return doRebuildAndReturn(productId, l1Key, l2Key);
            } finally {
                local.unlock();
            }
        }
        try {
            // 拿到锁后双检缓存，避免重复打库
            Result<?> doubleCheck = l1Cache.getIfPresent(l1Key);
            if (doubleCheck != null) {
                return doubleCheck;
            }
            return doRebuildAndReturn(productId, l1Key, l2Key);
        } finally {
            redisLockUtil.unlock(lockKey, requestId);
        }
    }

    /** 查库并回填缓存（含空值缓存与 TTL 抖动） */
    private Result<?> doRebuildAndReturn(Long productId, String l1Key, String l2Key) {
        Product product = productMapper.selectById(productId);
        if (product != null) {
            product.setMerchant(userMapper.selectById(product.getMerchantId()));
            Category category = categoryMapper.selectById(product.getCategoryId());
            if (category != null) {
                product.setCategory(category);
            }
            // 商品存在 → 加入布隆过滤器，回填缓存
            if (enabled && bloomFilter != null) {
                bloomFilter.add(String.valueOf(productId));
            }
            Result<?> result = Result.success(product);
            long ttl = jitterTtl(l2TtlSeconds);
            l1Cache.put(l1Key, result);
            writeToRedis(l2Key, result, ttl);
            return result;
        }
        // 商品不存在 → 写短 TTL 空标记（穿透兜底）
        Result<?> notFound = Result.error("-1", "未找到商品");
        l1Cache.put(NULL_MARKER + productId, notFound);
        writeToRedis(NULL_MARKER + productId, notFound, nullTtlSeconds);
        return notFound;
    }

    /** 雪崩防护：TTL 叠加随机抖动 */
    private long jitterTtl(long baseSeconds) {
        if (ttlJitterSeconds <= 0) {
            return baseSeconds;
        }
        return baseSeconds + ThreadLocalRandom.current().nextLong(ttlJitterSeconds + 1);
    }

    private Result<?> readFromRedis(String key) {
        try {
            String raw = stringRedisTemplate.opsForValue().get(key);
            if (raw == null) {
                return null;
            }
            if (NULL_MARKER.equals(raw)) {
                return Result.error("-1", "未找到商品");
            }
            return objectMapper.readValue(raw, Result.class);
        } catch (Exception e) {
            LOGGER.warn("读取 Redis 缓存失败 key={} err={}", key, e.getMessage());
            return null;
        }
    }

    private void writeToRedis(String key, Result<?> result, long ttlSeconds) {
        if (!l2Enabled || stringRedisTemplate == null) {
            return;
        }
        try {
            String raw = NULL_MARKER.equals(result.getMsg()) && result.getCode() != null
                    ? NULL_MARKER : objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(key, raw, Duration.ofSeconds(Math.max(ttlSeconds, 1)));
        } catch (Exception e) {
            LOGGER.warn("写入 Redis 缓存失败 key={} err={}", key, e.getMessage());
        }
    }

    /** 失效缓存（商品变更时调用） */
    public void evict(Long productId) {
        String key = L2_KEY_PREFIX + productId;
        l1Cache.invalidate(key);
        l1Cache.invalidate(NULL_MARKER + productId);
        if (l2Enabled && stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(key);
                stringRedisTemplate.delete(NULL_MARKER + productId);
            } catch (Exception e) {
                LOGGER.warn("删除 Redis 缓存失败 key={} err={}", key, e.getMessage());
            }
        }
    }
}
