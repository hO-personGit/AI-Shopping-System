package org.example.springboot.ratelimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * {@link RateLimit} 注解切面：调用前从令牌桶取令牌，不足则抛 {@link RateLimitExceededException}。
 */
@Aspect
@Component
public class RateLimitAspect {

    @Resource
    private TokenBucketRateLimiter rateLimiter;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = resolveKey(joinPoint, rateLimit);
        if (!rateLimiter.tryAcquire(key, rateLimit.permitsPerSecond(), rateLimit.capacity())) {
            throw new RateLimitExceededException("请求过于频繁，请稍后重试");
        }
        return joinPoint.proceed();
    }

    /**
     * 解析限流 key：注解显式 key 优先；支持 "{0}" 占位引用第一个参数；
     * 默认使用方法签名（类名#方法名）。
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        String key = rateLimit.key();
        if (key == null || key.isEmpty()) {
            return joinPoint.getSignature().toShortString();
        }
        if (key.contains("{0}") && joinPoint.getArgs().length > 0
                && joinPoint.getSignature() instanceof MethodSignature ms) {
            Object arg = joinPoint.getArgs()[0];
            if (arg != null) {
                return key.replace("{0}", String.valueOf(arg));
            }
        }
        return key;
    }
}
