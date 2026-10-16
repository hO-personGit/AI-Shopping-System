package org.example.springboot.ratelimit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * {@link CircuitBreaker} 注解切面：调用前检查熔断状态，执行后记录结果。
 * 熔断打开时抛 {@link CircuitBreakerOpenException}，由调用方降级。
 */
@Aspect
@Component
public class CircuitBreakerAspect {

    @Resource
    private CircuitBreakerRegistry registry;

    @Around("@annotation(circuitBreaker)")
    public Object around(ProceedingJoinPoint joinPoint, CircuitBreaker circuitBreaker) throws Throwable {
        String name = circuitBreaker.name();
        if (name == null || name.isEmpty()) {
            name = joinPoint.getSignature().toShortString();
        }
        CircuitBreakerState state = registry.get(name, circuitBreaker.failureThreshold(),
                circuitBreaker.windowSeconds(), circuitBreaker.openSeconds(),
                circuitBreaker.failureRateThreshold());

        if (!state.allowRequest()) {
            throw new CircuitBreakerOpenException("服务暂不可用，请稍后重试");
        }
        try {
            Object result = joinPoint.proceed();
            state.recordResult(true);
            return result;
        } catch (Throwable t) {
            state.recordResult(false);
            throw t;
        }
    }
}
