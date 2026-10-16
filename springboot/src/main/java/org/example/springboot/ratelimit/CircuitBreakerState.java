package org.example.springboot.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 单实例熔断状态（滑动窗口统计 + 闭/开/半开三态状态机）。
 *
 * <p>状态机：
 * <ul>
 *   <li>CLOSED：正常放行，记录窗口内失败时间戳；失败次数 ≥ failureThreshold 或失败率 ≥ failureRateThreshold → OPEN。</li>
 *   <li>OPEN：拒绝全部请求；持续 openSeconds 后进入 HALF_OPEN。</li>
 *   <li>HALF_OPEN：放行单个探测请求；成功 → CLOSED（清空统计），失败 → OPEN（重新计时）。</li>
 * </ul>
 */
public class CircuitBreakerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakerState.class);

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold;
    private final long windowNanos;
    private final long openNanos;
    private final double failureRateThreshold;

    /** 滑动窗口内的失败时间戳（纳秒）。 */
    private final Deque<Long> failures = new ArrayDeque<>();
    /** 滑动窗口内的总调用次数（用于失败率）。 */
    private long windowCalls;
    /** 当前半开探测是否已放行（同一时刻只放一个探测）。 */
    private boolean halfOpenProbeSent;

    private volatile State state = State.CLOSED;
    private volatile long openedAtNanos;

    public CircuitBreakerState(String name, int failureThreshold, long windowSeconds,
                               long openSeconds, double failureRateThreshold) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.windowNanos = windowSeconds * 1_000_000_000L;
        this.openNanos = openSeconds * 1_000_000_000L;
        this.failureRateThreshold = failureRateThreshold;
    }

    /**
     * 尝试放行请求。返回 false 表示熔断拒绝。
     */
    public synchronized boolean allowRequest() {
        long now = System.nanoTime();
        switch (state) {
            case CLOSED:
                evictExpired(now);
                return true;
            case OPEN:
                if (now - openedAtNanos >= openNanos) {
                    state = State.HALF_OPEN;
                    halfOpenProbeSent = false;
                    LOGGER.info("熔断器 {} 由 OPEN 进入 HALF_OPEN，放行探测请求", name);
                } else {
                    return false;
                }
                // fallthrough：半开放行逻辑
            case HALF_OPEN:
                if (halfOpenProbeSent) {
                    return false; // 探测未返回前拒绝其余请求
                }
                halfOpenProbeSent = true;
                return true;
            default:
                return false;
        }
    }

    /**
     * 记录一次调用结果。
     */
    public synchronized void recordResult(boolean success) {
        long now = System.nanoTime();
        switch (state) {
            case HALF_OPEN:
                if (success) {
                    resetToClosed(now);
                } else {
                    state = State.OPEN;
                    openedAtNanos = now;
                    LOGGER.warn("熔断器 {} 半开探测失败，回到 OPEN", name);
                }
                break;
            case CLOSED:
            default:
                evictExpired(now);
                windowCalls++;
                if (!success) {
                    failures.addLast(now);
                    double failureRate = windowCalls == 0 ? 0 : (double) failures.size() / windowCalls;
                    if (failures.size() >= failureThreshold || failureRate >= failureRateThreshold) {
                        state = State.OPEN;
                        openedAtNanos = now;
                        LOGGER.warn("熔断器 {} 触发熔断：窗口失败 {} 次 / 调用 {} 次（阈值 {} 次 / {}%）",
                                name, failures.size(), windowCalls, failureThreshold,
                                (int) (failureRateThreshold * 100));
                    }
                }
                break;
        }
    }

    private void evictExpired(long now) {
        while (!failures.isEmpty() && now - failures.peekFirst() > windowNanos) {
            failures.removeFirst();
            windowCalls = Math.max(0, windowCalls - 1);
        }
    }

    private void resetToClosed(long now) {
        state = State.CLOSED;
        failures.clear();
        windowCalls = 0;
        halfOpenProbeSent = false;
        LOGGER.info("熔断器 {} 恢复正常（CLOSED）", name);
    }

    public State getState() {
        return state;
    }
}
