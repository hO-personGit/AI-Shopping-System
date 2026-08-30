package org.example.springboot.mq;

import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.enumClass.OrderStatus;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.util.RedisStockDeductionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 订单消息处理器单元测试：覆盖幂等落库、支付幂等、超时关单与取消回补。
 */
@ExtendWith(MockitoExtension.class)
class OrderMessageProcessorTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private RedisStockDeductionUtil stockDeductionUtil;
    @Mock
    private OrderMessageProducer producer;

    @InjectMocks
    private OrderMessageProcessor processor;

    private OrderMessage buildCreateMessage() {
        return OrderMessage.builder()
                .eventType(OrderEventType.ORDER_CREATE.name())
                .orderNo("20260830010101" + "123456")
                .userId(1L)
                .productId(10L)
                .quantity(2)
                .price(BigDecimal.valueOf(99))
                .totalPrice(BigDecimal.valueOf(198))
                .recvName("张三")
                .recvAddress("郑州")
                .recvPhone("13000000000")
                .build();
    }

    @Test
    void handleCreate_shouldInsertOrderAndScheduleTimeout() {
        OrderMessage msg = buildCreateMessage();
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(orderMapper.insert(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(100L);
            return 1;
        });

        boolean ok = processor.handle(msg);

        assertTrue(ok);
        verify(orderMapper, times(1)).insert(any(Order.class));
        // 落库成功后应投递延迟关单消息
        verify(producer, times(1)).publish(any(OrderMessage.class), eq(true));
    }

    @Test
    void handleCreate_shouldBeIdempotentWhenOrderExists() {
        OrderMessage msg = buildCreateMessage();
        Order exists = new Order();
        exists.setId(100L);
        exists.setOrderNo(msg.getOrderNo());
        when(orderMapper.selectOne(any())).thenReturn(exists);

        boolean ok = processor.handle(msg);

        assertTrue(ok);
        verify(orderMapper, never()).insert(any(Order.class));
        verify(producer, never()).publish(any(OrderMessage.class), anyBoolean());
    }

    @Test
    void handlePaySuccess_shouldSkipWhenAlreadyPaid() {
        OrderMessage msg = OrderMessage.builder()
                .eventType(OrderEventType.ORDER_PAY_SUCCESS.name())
                .orderId(100L)
                .build();
        Order paid = new Order();
        paid.setId(100L);
        paid.setStatus(OrderStatus.PAID.getCode());
        when(orderMapper.selectById(100L)).thenReturn(paid);

        boolean ok = processor.handle(msg);

        assertTrue(ok);
        verify(productMapper, never()).deductStock(anyLong(), anyInt());
    }

    @Test
    void handleCancel_shouldRestoreRedisStock() {
        OrderMessage msg = OrderMessage.builder()
                .eventType(OrderEventType.ORDER_CANCEL.name())
                .orderId(100L)
                .orderNo("NO100")
                .productId(10L)
                .quantity(2)
                .build();
        Order pending = new Order();
        pending.setId(100L);
        pending.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        when(orderMapper.selectById(100L)).thenReturn(pending);

        boolean ok = processor.handle(msg);

        assertTrue(ok);
        assertEquals(OrderStatus.CANCELLED.getCode(), pending.getStatus());
        verify(stockDeductionUtil, times(1)).restore(10L, 2);
    }

    @Test
    void handleTimeoutClose_shouldSkipWhenAlreadyPaid() {
        OrderMessage msg = OrderMessage.builder()
                .eventType(OrderEventType.ORDER_TIMEOUT_CLOSE.name())
                .orderId(100L)
                .productId(10L)
                .quantity(1)
                .build();
        Order paid = new Order();
        paid.setId(100L);
        paid.setStatus(OrderStatus.PAID.getCode());
        when(orderMapper.selectById(100L)).thenReturn(paid);

        boolean ok = processor.handle(msg);

        assertTrue(ok);
        // 已支付订单不可被超时关闭，且不回补库存
        verify(orderMapper, never()).updateById(any(Order.class));
        verify(stockDeductionUtil, never()).restore(anyLong(), anyInt());
    }

    @Test
    void handle_shouldRejectUnknownEvent() {
        OrderMessage msg = OrderMessage.builder().eventType("UNKNOWN_EVENT").build();
        boolean ok = processor.handle(msg);
        assertFalse(ok);
    }
}
