package org.example.springboot.idgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法分布式 ID 生成器。
 *
 * <p>64 位长整型布局：1 位符号 + 41 位毫秒时间戳 + 5 位数据中心 + 5 位机器 + 12 位序列。
 * <ul>
 *   <li>趋势递增、全局唯一、无需依赖数据库/Redis，适合订单号等业务主键场景；</li>
 *   <li>workerId / datacenterId 通过配置注入，多实例部署时按实例分配即可避免重复；</li>
 *   <li>时钟回拨检测：回拨超过阈值直接抛异常，小回拨等待恢复。</li>
 * </ul>
 */
@Component
public class SnowflakeIdGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    // ================== 位数定义 ==================
    private static final long START_EPOCH = 1704067200000L; // 2024-01-01 00:00:00
    private static final long SEQUENCE_BITS = 12L;
    private static final long WORKER_BITS = 5L;
    private static final long DATACENTER_BITS = 5L;
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long MAX_WORKER = ~(-1L << WORKER_BITS);
    private static final long MAX_DATACENTER = ~(-1L << DATACENTER_BITS);
    private static final long WORKER_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_SHIFT = SEQUENCE_BITS + WORKER_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_BITS + DATACENTER_BITS;

    /** 时钟回拨容忍上限（毫秒），超过则抛异常。 */
    private static final long MAX_CLOCK_BACKWARD_MS = 5L;

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(
            @Value("${idgen.worker-id:0}") long workerId,
            @Value("${idgen.datacenter-id:0}") long datacenterId) {
        if (workerId > MAX_WORKER || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出范围 [0, " + MAX_WORKER + "]");
        }
        if (datacenterId > MAX_DATACENTER || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 超出范围 [0, " + MAX_DATACENTER + "]");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        LOGGER.info("SnowflakeIdGenerator 初始化 workerId={} datacenterId={}", workerId, datacenterId);
    }

    /**
     * 生成下一个全局唯一 ID。
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();
        if (timestamp < lastTimestamp) {
            long backward = lastTimestamp - timestamp;
            if (backward > MAX_CLOCK_BACKWARD_MS) {
                throw new IllegalStateException("时钟回拨超过容忍上限 " + backward + "ms，拒绝生成 ID");
            }
            // 小回拨：等待追上上次时间
            timestamp = waitUntil(lastTimestamp);
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitUntil(lastTimestamp + 1);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_SHIFT)
                | (workerId << WORKER_SHIFT)
                | sequence;
    }

    /** 生成字符串形式的 ID（订单号等场景）。 */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    private long waitUntil(long targetTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp < targetTimestamp) {
            Thread.onSpinWait();
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
