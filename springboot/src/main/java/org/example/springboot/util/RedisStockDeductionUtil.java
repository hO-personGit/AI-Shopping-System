package org.example.springboot.util;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Redis 库存预扣工具（基于 Lua 脚本原子操作）。
 *
 * <p>用于下单场景的高并发防超卖：
 * <ul>
 *   <li>预扣：<code>DECRBY</code> 前判断剩余可售库存，不足则回滚，整个判断+扣减在 Lua 中原子完成。</li>
 *   <li>回补：关单/取消时 <code>INCRBY</code> 恢复 Redis 预扣库存。</li>
 *   <li>容错：Redis 不可用时返回失败，由调用方走同步兜底，保证演示环境可运行。</li>
 * </ul>
 */
@Component
public class RedisStockDeductionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStockDeductionUtil.class);

    /** Redis 预扣库存键前缀：aps:stock:{productId} */
    public static final String STOCK_KEY_PREFIX = "aps:stock:";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 原子预扣库存（含初始化兜底）。
     *
     * <p>Lua：key 不存在时用传入的初始库存初始化，再 DECRBY；结果小于 0 时回滚并返回失败。
     *
     * @param productId       商品 ID
     * @param quantity        预扣数量
     * @param dbStockFallback 数据库当前库存（key 不存在时用于初始化）
     * @return true 预扣成功
     */
    public boolean tryDeduct(Long productId, Integer quantity, Integer dbStockFallback) {
        if (productId == null || quantity == null || quantity <= 0) {
            return false;
        }
        String key = STOCK_KEY_PREFIX + productId;
        String script = """
                local stock = redis.call('GET', KEYS[1])
                if not stock then
                    redis.call('SET', KEYS[1], ARGV[2])
                    stock = ARGV[2]
                end
                local remain = tonumber(stock) - tonumber(ARGV[1])
                if remain < 0 then
                    return -1
                end
                redis.call('SET', KEYS[1], remain)
                return remain
                """;
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
            Long remain = stringRedisTemplate.execute(redisScript, List.of(key),
                    String.valueOf(quantity), String.valueOf(dbStockFallback == null ? 0 : dbStockFallback));
            return remain != null && remain >= 0;
        } catch (Exception e) {
            LOGGER.warn("Redis 预扣库存失败，回退同步校验 productId={} err={}", productId, e.getMessage());
            return false;
        }
    }

    /**
     * 回补 Redis 预扣库存（关单 / 取消 / 退款时调用）。
     */
    public void restore(Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity <= 0) {
            return;
        }
        String key = STOCK_KEY_PREFIX + productId;
        try {
            stringRedisTemplate.opsForValue().increment(key, quantity);
        } catch (Exception e) {
            LOGGER.warn("Redis 回补库存失败 productId={} err={}", productId, e.getMessage());
        }
    }

    /**
     * 查询 Redis 预扣库存（不存在返回 null）。
     */
    public Integer getRemainStock(Long productId) {
        if (productId == null) {
            return null;
        }
        try {
            String val = stringRedisTemplate.opsForValue().get(STOCK_KEY_PREFIX + productId);
            return val == null ? null : Integer.parseInt(val);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 删除预扣库存键（支付完成后可释放）。
     */
    public void remove(Long productId) {
        if (productId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(STOCK_KEY_PREFIX + productId);
        } catch (Exception e) {
            LOGGER.warn("Redis 删除预扣库存失败 productId={} err={}", productId, e.getMessage());
        }
    }
}
