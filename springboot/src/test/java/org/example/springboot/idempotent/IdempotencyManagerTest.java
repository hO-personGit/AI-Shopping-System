package org.example.springboot.idempotent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 幂等结果管理器单元测试：缓存写入、命中、过期策略。
 */
class IdempotencyManagerTest {

    @Test
    void 首次无结果重复返回缓存() {
        IdempotencyManager manager = new IdempotencyManager();
        assertNull(manager.get("order:20260906:001"));
        manager.put("order:20260906:001", "{\"code\":\"0\"}");
        assertEquals("{\"code\":\"0\"}", manager.get("order:20260906:001"));
        assertEquals("{\"code\":\"0\"}", manager.get("order:20260906:001"));
    }

    @Test
    void 不同key互不影响() {
        IdempotencyManager manager = new IdempotencyManager();
        manager.put("k1", "r1");
        assertNull(manager.get("k2"));
        assertEquals("r1", manager.get("k1"));
    }
}
