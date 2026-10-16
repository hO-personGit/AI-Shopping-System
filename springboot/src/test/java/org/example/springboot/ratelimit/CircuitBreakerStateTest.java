package org.example.springboot.ratelimit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 熔断状态机单元测试：失败阈值触发、OPEN 拒绝、半开探测、恢复关闭。
 */
class CircuitBreakerStateTest {

    private CircuitBreakerState newState() {
        return new CircuitBreakerState("test", 3, 10, 1, 0.5);
    }

    @Test
    void 正常调用不熔断() {
        CircuitBreakerState state = newState();
        for (int i = 0; i < 20; i++) {
            assertTrue(state.allowRequest());
            state.recordResult(true);
        }
        assertEquals(CircuitBreakerState.State.CLOSED, state.getState());
    }

    @Test
    void 连续失败达到阈值触发熔断() {
        CircuitBreakerState state = newState();
        // 2 次失败（失败率 100% ≥ 50% 提前触发）→ 熔断
        state.recordResult(false);
        state.recordResult(false);
        assertEquals(CircuitBreakerState.State.OPEN, state.getState(), "失败率超阈值应熔断");
        assertFalse(state.allowRequest(), "OPEN 状态应拒绝请求");
    }

    @Test
    void 熔断到期后半开探测成功恢复关闭() throws InterruptedException {
        CircuitBreakerState state = newState();
        state.recordResult(false);
        state.recordResult(false);
        assertEquals(CircuitBreakerState.State.OPEN, state.getState());

        Thread.sleep(1100); // openSeconds=1s 到期
        assertTrue(state.allowRequest(), "到期后应放行半开探测");
        state.recordResult(true);
        assertEquals(CircuitBreakerState.State.CLOSED, state.getState(), "探测成功应恢复 CLOSED");
        assertTrue(state.allowRequest());
    }

    @Test
    void 半开探测失败回到熔断() throws InterruptedException {
        CircuitBreakerState state = newState();
        state.recordResult(false);
        state.recordResult(false);
        Thread.sleep(1100);
        assertTrue(state.allowRequest());
        state.recordResult(false);
        assertEquals(CircuitBreakerState.State.OPEN, state.getState(), "探测失败应回到 OPEN");
        assertFalse(state.allowRequest());
    }
}
