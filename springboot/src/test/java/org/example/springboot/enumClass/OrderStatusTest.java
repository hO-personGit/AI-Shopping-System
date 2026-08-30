package org.example.springboot.enumClass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 订单状态机单元测试：覆盖合法流转、非法流转与状态解析。
 */
class OrderStatusTest {

    @Test
    void legalTransitions_shouldPass() {
        assertTrue(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID), "待支付→已支付");
        assertTrue(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.CANCELLED), "待支付→已取消");
        assertTrue(OrderStatus.canTransition(OrderStatus.PAID, OrderStatus.SHIPPED), "已支付→已发货");
        assertTrue(OrderStatus.canTransition(OrderStatus.SHIPPED, OrderStatus.COMPLETED), "已发货→已完成");
        assertTrue(OrderStatus.canTransition(OrderStatus.PAID, OrderStatus.REFUNDING), "已支付→退款中");
        assertTrue(OrderStatus.canTransition(OrderStatus.REFUNDING, OrderStatus.REFUNDED), "退款中→已退款");
        assertTrue(OrderStatus.canTransition(OrderStatus.REFUNDING, OrderStatus.REFUND_FAILED), "退款中→退款失败");
    }

    @Test
    void illegalTransitions_shouldReject() {
        assertFalse(OrderStatus.canTransition(OrderStatus.PENDING_PAYMENT, OrderStatus.COMPLETED), "待支付→已完成 非法");
        assertFalse(OrderStatus.canTransition(OrderStatus.CANCELLED, OrderStatus.PAID), "已取消→已支付 非法");
        assertFalse(OrderStatus.canTransition(OrderStatus.COMPLETED, OrderStatus.PAID), "已完成→已支付 非法");
        assertFalse(OrderStatus.canTransition(OrderStatus.REFUNDED, OrderStatus.PAID), "已退款→已支付 非法");
        assertFalse(OrderStatus.canTransition(OrderStatus.PAID, OrderStatus.CANCELLED), "已支付→已取消 非法（应走退款）");
    }

    @Test
    void byCodeTransition_shouldWork() {
        assertTrue(OrderStatus.canTransition(0, 1));
        assertTrue(OrderStatus.canTransition(0, 4));
        assertFalse(OrderStatus.canTransition(1, 4));
        assertFalse(OrderStatus.canTransition(3, 0));
    }

    @Test
    void fromCode_shouldParse() {
        assertEquals(OrderStatus.PENDING_PAYMENT, OrderStatus.fromCode(0));
        assertEquals(OrderStatus.PAID, OrderStatus.fromCode(1));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromCode(4));
        assertEquals(OrderStatus.REFUNDED, OrderStatus.fromCode(6));
        assertNull(OrderStatus.fromCode(99));
        assertNull(OrderStatus.fromCode(null));
    }

    @Test
    void validateTransition_shouldReturnTargetOrNull() {
        assertEquals(OrderStatus.PAID, OrderStatus.validateTransition(0, 1));
        assertEquals(OrderStatus.CANCELLED, OrderStatus.validateTransition(0, 4));
        assertNull(OrderStatus.validateTransition(1, 4));
        assertNull(OrderStatus.validateTransition(0, 3));
    }
}
