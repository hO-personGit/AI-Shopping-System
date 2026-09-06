package org.example.springboot.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级熔断注解（滑动窗口 + 三态状态机）。
 *
 * <p>通过 {@link CircuitBreakerAspect} 切面生效：CLOSED（闭）→ 失败达到阈值进入
 * OPEN（开）→ 熔断时长后进入 HALF_OPEN（半开）放行探测请求 → 成功回 CLOSED / 失败回 OPEN。
 * 熔断期间直接抛出 {@link CircuitBreakerOpenException}（HTTP 503），由调用方降级。
 *
 * <p>用法：
 * <pre>{@code
 * @CircuitBreaker(name = "ai-service", failureThreshold = 5, windowSeconds = 10, openSeconds = 15)
 * public AiGuideResponse smartGuide(AiGuideRequest request) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {

    /** 熔断器名称（空时默认使用方法签名），同一名称共享状态。 */
    String name() default "";

    /** 滑动窗口内失败次数阈值，达到即熔断。 */
    int failureThreshold() default 5;

    /** 滑动窗口时长（秒），窗口外失败自动过期。 */
    long windowSeconds() default 10;

    /** 熔断持续时间（秒），到期进入半开。 */
    long openSeconds() default 15;

    /** 失败率阈值（0~1），窗口内失败率超过该值也触发熔断。 */
    double failureRateThreshold() default 0.5;
}
