package org.example.springboot.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口幂等注解。
 *
 * <p>由 {@link IdempotentAspect} 切面生效：以「方法签名 + 请求体 requestId」为幂等键，
 * 首次执行缓存返回结果（JSON），窗口内相同 requestId 的重复请求直接返回首次结果，不再执行业务。
 * 适用于下单、支付等「重复提交会造成重复扣款/重复建单」的写接口。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /** 幂等窗口时长（秒），默认 10 分钟。 */
    long expireSeconds() default 600;
}
