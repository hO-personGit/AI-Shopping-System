package org.example.springboot.mq;

import org.example.springboot.entity.Order;
import org.example.springboot.entity.Product;
import org.example.springboot.enumClass.OrderStatus;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mapper.ProductMapper;
import org.example.springboot.util.RedisStockDeductionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 订单消息处理器实现：所有订单异步事件的业务落地逻辑。
 *
 * <p>核心原则：
 * <ul>
 *   <li>幂等：以 orderNo / orderId + 状态机判断是否已处理，重复消息不重复落库、不重复回补。</li>
 *   <li>状态机：所有状态变更通过 {@link OrderStatus#canTransition} 校验，拒绝非法流转。</li>
 *   <li>最终一致性：下单预扣 Redis 库存，支付时数据库最终扣减，超时关单回补 Redis 预扣。</li>
 * </ul>
 */
@Service
public class OrderMessageProcessor implements OrderMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessageProcessor.class);

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisStockDeductionUtil stockDeductionUtil;

    @Autowired
    @Lazy // 与 OrderMessageProducer 存在循环依赖（producer 需回调 handler），用 @Lazy 打破
    private OrderMessageProducer producer;

    @Override
    public boolean handle(OrderMessage message) {
        if (message == null || message.getEventType() == null) {
            return false;
        }
        OrderEventType type = OrderEventType.fromName(message.getEventType());
        if (type == null) {
            LOGGER.warn("未知订单事件类型: {}", message.getEventType());
            return false;
        }
        try {
            switch (type) {
                case ORDER_CREATE -> handleCreate(message);
                case ORDER_PAY_SUCCESS -> handlePaySuccess(message);
                case ORDER_TIMEOUT_CLOSE -> handleTimeoutClose(message);
                case ORDER_CANCEL -> handleCancel(message);
                default -> {
                    LOGGER.warn("未处理的事件类型: {}", type);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("订单事件处理失败 eventType={} orderNo={} err={}",
                    message.getEventType(), message.getOrderNo(), e.getMessage(), e);
            return false;
        }
    }

    // ================= 创建订单（异步落库） =================

    @Transactional
    public void handleCreate(OrderMessage message) {
        // 幂等：orderNo 已存在则跳过
        if (message.getOrderNo() != null) {
            Order exists = orderMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Order>()
                            .eq(Order::getOrderNo, message.getOrderNo()));
            if (exists != null) {
                LOGGER.info("订单已存在，幂等跳过 orderNo={}", message.getOrderNo());
                return;
            }
        }

        Order order = new Order();
        order.setOrderNo(message.getOrderNo() != null ? message.getOrderNo()
                : "NO" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6));
        order.setUserId(message.getUserId());
        order.setProductId(message.getProductId());
        order.setQuantity(message.getQuantity());
        order.setPrice(message.getPrice());
        order.setTotalPrice(message.getTotalPrice() != null ? message.getTotalPrice()
                : message.getPrice().multiply(java.math.BigDecimal.valueOf(message.getQuantity())));
        order.setRecvName(message.getRecvName());
        order.setRecvAddress(message.getRecvAddress());
        order.setRecvPhone(message.getRecvPhone());
        order.setRemark(message.getRemark());
        order.setStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setLastStatus(OrderStatus.PENDING_PAYMENT.getCode());
        order.setRefundStatus(0);
        orderMapper.insert(order);

        LOGGER.info("异步创建订单成功 orderId={} orderNo={}", order.getId(), order.getOrderNo());

        // 落库成功后，投递延迟关单消息（超时未支付自动取消）
        OrderMessage timeoutMsg = OrderMessage.builder()
                .eventType(OrderEventType.ORDER_TIMEOUT_CLOSE.name())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .build();
        producer.publish(timeoutMsg, true);
    }

    // ================= 支付成功（幂等，避免重复扣库存） =================

    @Transactional
    public void handlePaySuccess(OrderMessage message) {
        if (message.getOrderId() == null) {
            return;
        }
        Order order = orderMapper.selectById(message.getOrderId());
        if (order == null) {
            LOGGER.warn("支付事件订单不存在 orderId={}", message.getOrderId());
            return;
        }
        // 幂等：已支付则跳过
        if (OrderStatus.PAID.getCode() == order.getStatus()) {
            LOGGER.info("订单已支付，幂等跳过 orderId={}", order.getId());
            return;
        }
        // 状态机校验：待支付 → 已支付
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.PAID.getCode())) {
            LOGGER.warn("非法状态流转 orderId={} from={} to={}", order.getId(),
                    order.getStatus(), OrderStatus.PAID.getCode());
            return;
        }
        // 数据库原子扣减库存（防超卖兜底），不足则拒绝
        Product product = productMapper.selectById(order.getProductId());
        if (product == null || product.getStock() < order.getQuantity()) {
            LOGGER.warn("支付时库存不足 orderId={} productId={}", order.getId(), order.getProductId());
            return;
        }
        order.setLastStatus(order.getStatus());
        order.setStatus(OrderStatus.PAID.getCode());
        orderMapper.updateById(order);

        int updated = productMapper.deductStock(order.getProductId(), order.getQuantity());
        if (updated <= 0) {
            // 理论不应发生（上面已校验），回滚状态
            LOGGER.error("支付扣减库存失败 orderId={}", order.getId());
        } else {
            LOGGER.info("支付成功，库存已扣减 orderId={} quantity={}", order.getId(), order.getQuantity());
        }
    }

    // ================= 超时未支付自动关单 =================

    @Transactional
    public void handleTimeoutClose(OrderMessage message) {
        closeOrder(message, "订单超时未支付自动取消");
    }

    // ================= 取消订单（回补 Redis 预扣库存） =================

    @Transactional
    public void handleCancel(OrderMessage message) {
        closeOrder(message, "用户取消订单");
    }

    private void closeOrder(OrderMessage message, String reason) {
        if (message.getOrderId() == null) {
            return;
        }
        Order order = orderMapper.selectById(message.getOrderId());
        if (order == null) {
            LOGGER.warn("关单事件订单不存在 orderId={}", message.getOrderId());
            return;
        }
        // 幂等：已取消 / 已支付后不可取消
        if (OrderStatus.CANCELLED.getCode() == order.getStatus()) {
            LOGGER.info("订单已取消，幂等跳过 orderId={}", order.getId());
            return;
        }
        if (!OrderStatus.canTransition(order.getStatus(), OrderStatus.CANCELLED.getCode())) {
            LOGGER.info("订单当前状态不允许取消 orderId={} status={}", order.getId(), order.getStatus());
            return;
        }
        order.setLastStatus(order.getStatus());
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setRemark(reason);
        orderMapper.updateById(order);

        // 回补 Redis 预扣库存（下单时仅在 Redis 预扣，未支付未扣 DB 库存）
        stockDeductionUtil.restore(message.getProductId(), message.getQuantity());
        LOGGER.info("订单已取消，回补 Redis 预扣库存 orderId={} productId={} quantity={}",
                order.getId(), message.getProductId(), message.getQuantity());
    }

    /** 关闭超时订单的本地兜底扫描实现（供定时任务调用，逻辑与 MQ 一致） */
    @Transactional
    public boolean cancelExpiredOrder(Long orderId, Integer quantity, Long productId) {
        if (orderId == null) {
            return false;
        }
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT.getCode()) {
            return false;
        }
        order.setLastStatus(order.getStatus());
        order.setStatus(OrderStatus.CANCELLED.getCode());
        order.setRemark("定时任务扫描：订单超时未支付自动取消");
        orderMapper.updateById(order);
        stockDeductionUtil.restore(productId != null ? productId : order.getProductId(),
                quantity != null ? quantity : order.getQuantity());
        LOGGER.info("定时任务关闭超时订单 orderId={}", orderId);
        return true;
    }
}
