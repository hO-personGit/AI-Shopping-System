package org.example.springboot.common;

/**
 * 业务异常：由全局异常处理器统一转换为 Result 响应。
 *
 * <p>使用方式：在 Service/Controller 抛出，GlobalExceptionHandler 统一捕获并返回
 * {@code Result.error(code, message)}，避免散落的 try-catch 与错误码不一致。
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        this("-1", message);
    }

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
