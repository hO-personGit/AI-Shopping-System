package org.example.springboot.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级限流注解（令牌桶）。
 *
 * <p>通过 {@link RateLimitAspect} 切面生效：按 key 维度进行令牌桶限流，
 * 令牌不足时抛出 {@link RateLimitExceededException}，由全局异常处理器转换为 429。
 *
 * <p>用法：
 * <pre>{@code
 * @RateLimit(key = "product-detail", permitsPerSecond = 200, capacity = 300)
 * public Result<?> getProductById(@PathVariable Long id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /** 限流 key（空时默认使用方法签名）。支持 "{0}" 占位引用第一个参数，如 "user:{0}"。 */
    String key() default "";

    /** 令牌补充速率（每秒补充令牌数）。 */
    double permitsPerSecond() default 100;

    /** 桶容量（允许的瞬时突发量）。 */
    double capacity() default 200;
}
