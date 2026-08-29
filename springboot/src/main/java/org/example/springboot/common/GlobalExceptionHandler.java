package org.example.springboot.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * 全局异常处理器：统一捕获异常并转换为 {@link Result} 响应。
 *
 * <p>覆盖范围：
 * <ul>
 *   <li>业务异常 BusinessException → 返回业务错误码。</li>
 *   <li>参数校验异常（@Valid / @RequestBody）→ 返回 400。</li>
 *   <li>请求体解析失败 / 路径不存在 → 返回 400 / 404。</li>
 *   <li>兜底异常 → 记录日志并返回 500，避免堆栈泄露给前端。</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException ex) {
        return Result.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidationException(Exception ex) {
        String message = "参数校验失败";
        if (ex instanceof MethodArgumentNotValidException mve && mve.getBindingResult().hasErrors()) {
            message = mve.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        } else if (ex instanceof BindException be && be.getBindingResult().hasErrors()) {
            message = be.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        }
        return Result.error("400", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException ex) {
        return Result.error("400", "请求体格式错误：" + ex.getMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result<?> handleNotFound(NoHandlerFoundException ex) {
        return Result.error("404", "接口不存在：" + ex.getRequestURL());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        LOGGER.error("系统异常", ex);
        return Result.error("500", "系统繁忙，请稍后重试");
    }
}
