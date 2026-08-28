package org.example.springboot.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** 布隆过滤器单元测试：基本判存在 + 误判率上限 + 容量分配不爆内存。 */
class ProductBloomFilterTest {

    @Test
    void shouldContainAddedElements() {
        ProductBloomFilter filter = new ProductBloomFilter(20000, 0.01);
        for (int i = 1; i <= 1000; i++) {
            filter.add(String.valueOf(i));
        }
        for (int i = 1; i <= 1000; i++) {
            assertTrue(filter.contains(String.valueOf(i)), "已添加元素必须命中: " + i);
        }
    }

    @Test
    void shouldNotContainMostUnexistingElements() {
        ProductBloomFilter filter = new ProductBloomFilter(20000, 0.01);
        for (int i = 0; i < 5000; i++) {
            filter.add("p" + i);
        }
        // 从未添加的随机 ID，误判率应远低于 1%
        int falsePositive = 0;
        int total = 20000;
        Random random = new Random(42);
        for (int i = 0; i < total; i++) {
            long id = 100_000_000L + random.nextInt(1_000_000);
            if (filter.contains("p" + id)) {
                falsePositive++;
            }
        }
        double rate = falsePositive * 1.0 / total;
        assertTrue(rate < 0.02, "误判率超出预期: " + rate);
    }

    @Test
    void shouldAllocateReasonableMemory() {
        // 20000 元素规模位数组应远小于 1MB（此前 hutool 实现直接 OOM）
        ProductBloomFilter filter = new ProductBloomFilter(20000, 0.01);
        assertTrue(filter.bitSize() <= 5_000_000L, "位数组过大: " + filter.bitSize());
        assertTrue(filter.hashCount() >= 3, "哈希函数过少");
    }
}
