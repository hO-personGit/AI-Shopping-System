package org.example.springboot.service;

import org.example.springboot.common.Result;
import org.example.springboot.entity.Logistics;
import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.mapper.*;
import org.example.springboot.util.RedisLockUtil;
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
 * 订单服务单元测试：重点覆盖支付流程的分布式锁防超卖逻辑。
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private LogisticsMapper logisticsMapper;
    @Mock
    private AddressMapper addressMapper;
    @Mock
    private RedisLockUtil redisLockUtil;

    @InjectMocks
    private OrderService orderService;

    private Order buildOrder(Long id, Long productId, Integer quantity, BigDecimal price) {
        Order order = new Order();
        order.setId(id);
        order.setProductId(productId);
        order.setQuantity(quantity);
        order.setPrice(price);
        return order;
    }

    private Product buildProduct(Long id, Integer stock) {
        Product product = new Product();
        product.setId(id);
        product.setStock(stock);
        product.setSalesCount(0);
        return product;
    }

    @Test
    void payOrder_withValidLock_shouldDeductStockAndPay() {
        Order order = buildOrder(100L, 1L, 2, BigDecimal.valueOf(100));
        Product product = buildProduct(1L, 50);

        when(redisLockUtil.tryLock(anyString(), anyLong())).thenReturn("req-123");
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(productMapper.selectById(1L)).thenReturn(product);
        when(productMapper.updateById(any(Product.class))).thenReturn(1);

        Result<?> result = orderService.payOrder(100L);

        assertEquals("0", result.getCode());
        assertEquals(48, product.getStock(), "库存应扣减 2");
        assertEquals(2, product.getSalesCount(), "销量应增加 2");
        assertEquals(1, order.getStatus(), "订单状态应变为已支付(1)");
        verify(redisLockUtil).unlock(eq("lock:order:pay:product:100"), eq("req-123"));
    }

    @Test
    void payOrder_withInsufficientStock_shouldReject() {
        Order order = buildOrder(100L, 1L, 10, BigDecimal.valueOf(100));
        Product product = buildProduct(1L, 5);

        when(redisLockUtil.tryLock(anyString(), anyLong())).thenReturn("req-456");
        when(orderMapper.selectById(100L)).thenReturn(order);
        when(productMapper.selectById(1L)).thenReturn(product);

        Result<?> result = orderService.payOrder(100L);

        assertEquals("-1", result.getCode());
        assertEquals("库存不足", result.getMsg());
        verify(redisLockUtil).unlock(anyString(), eq("req-456"));
    }

    @Test
    void payOrder_whenLockBusy_shouldReturnBusy() {
        when(redisLockUtil.tryLock(anyString(), anyLong())).thenReturn(null);

        Result<?> result = orderService.payOrder(100L);

        assertEquals("-1", result.getCode());
        assertEquals("系统繁忙，请稍后重试", result.getMsg());
        // 未获取到锁，不应执行扣库存
        verify(productMapper, never()).updateById(any(Product.class));
        verify(redisLockUtil, never()).unlock(anyString(), any());
    }
}
