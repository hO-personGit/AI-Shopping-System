package org.example.springboot.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 统一响应体 Result 单元测试。
 */
class ResultTest {

    @Test
    void success_shouldSetCodeZero() {
        Result<?> result = Result.success();
        assertEquals("0", result.getCode());
        assertEquals("成功", result.getMsg());
    }

    @Test
    void success_withData_shouldCarryData() {
        Result<String> result = Result.success("hello");
        assertEquals("0", result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void error_shouldSetCodeAndMsg() {
        Result<?> result = Result.error("-1", "商品不存在");
        assertEquals("-1", result.getCode());
        assertEquals("商品不存在", result.getMsg());
        assertNull(result.getData());
    }

    @Test
    void error_withData_shouldCarryData() {
        Result<String> result = Result.error("500", "系统异常", "detail");
        assertEquals("500", result.getCode());
        assertEquals("detail", result.getData());
    }
}
