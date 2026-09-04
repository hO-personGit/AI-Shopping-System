package org.example.springboot.idempotent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * {@link Idempotent} 切面：从请求体（第一个参数）反射读取 requestId 构造幂等键，
 * 命中缓存直接返回首次结果对象（类型与业务方法返回一致，避免代理强转失败）。
 */
@Aspect
@Component
public class IdempotentAspect {

    @Resource
    private IdempotencyManager idempotencyManager;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String requestId = extractRequestId(args);
        if (requestId == null || requestId.isBlank()) {
            // 未携带幂等键：直接执行（幂等由业务层 orderNo 唯一键兜底）
            return joinPoint.proceed();
        }
        String key = joinPoint.getSignature().toShortString() + "::" + requestId;
        Object cached = idempotencyManager.get(key);
        if (cached != null) {
            return cached;
        }
        Object result = joinPoint.proceed();
        idempotencyManager.put(key, result);
        return result;
    }

    /** 约定：第一个参数若含 getRequestId() 方法则取之。 */
    private String extractRequestId(Object[] args) {
        if (args == null || args.length == 0 || args[0] == null) {
            return null;
        }
        try {
            Object first = args[0];
            var method = first.getClass().getMethod("getRequestId");
            Object value = method.invoke(first);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            return null;
        }
    }
}
