package org.example.springboot.ratelimit;

/**
 * 限流触发异常：请求超过令牌桶速率，由全局异常处理器转换为 HTTP 429。
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
