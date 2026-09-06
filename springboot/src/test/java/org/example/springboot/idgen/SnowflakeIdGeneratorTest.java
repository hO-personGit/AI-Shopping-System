package org.example.springboot.idgen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 雪花 ID 生成器单元测试：唯一性、趋势递增、位长符合 64 位。
 */
class SnowflakeIdGeneratorTest {

    @Test
    void 一万个ID全部唯一() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(1, 1);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            assertTrue(ids.add(generator.nextId()), "第 " + (i + 1) + " 个 ID 重复");
        }
        assertEquals(10_000, ids.size());
    }

    @Test
    void 同毫秒内序列不重复() throws InterruptedException {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, 0);
        Set<Long> ids = new HashSet<>();
        long start = System.currentTimeMillis();
        // 单线程快速生成 5000 个（必然落在同一毫秒多个序列）
        for (int i = 0; i < 5000; i++) {
            ids.add(generator.nextId());
        }
        assertEquals(5000, ids.size(), "同毫秒序列应互不重复");
        assertTrue(System.currentTimeMillis() - start < 5000, "生成应在 5s 内完成");
    }

    @Test
    void ID趋势递增() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(2, 3);
        long prev = generator.nextId();
        for (int i = 0; i < 1000; i++) {
            long cur = generator.nextId();
            assertTrue(cur > prev, "雪花 ID 应趋势递增");
            prev = cur;
        }
    }

    @Test
    void 字符串ID为纯数字() {
        SnowflakeIdGenerator generator = new SnowflakeIdGenerator(0, 0);
        String id = generator.nextIdStr();
        assertTrue(id.matches("\\d+"), "字符串 ID 应为纯数字，实际: " + id);
    }
}
