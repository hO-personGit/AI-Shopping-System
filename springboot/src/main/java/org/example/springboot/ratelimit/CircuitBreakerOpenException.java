package org.example.springboot.ratelimit;

/**
 * 熔断打开异常：熔断器处于 OPEN 状态时直接抛出，由调用方降级或全局处理器转 503。
 */
public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
