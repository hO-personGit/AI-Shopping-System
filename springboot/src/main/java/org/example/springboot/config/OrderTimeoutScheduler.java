package org.example.springboot.config;

import org.example.springboot.entity.Order;
import org.example.springboot.mq.OrderMessageProcessor;
import org.example.springboot.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

/**
 * 订单超时关单定时任务（延迟队列的兜底保障）。
 *
 * <p>即使 RabbitMQ 延迟队列不可用 / 消息丢失，也会每隔固定周期扫描
 * 待支付超时订单并自动取消、回补库存，保证「订单最终一致性」。
 */
@Configuration
public class OrderTimeoutScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMessageProcessor orderMessageProcessor;

    /** 超时时间（分钟），与延迟队列 TTL 保持一致 */
    @Value("${app.mq.order-timeout-minutes:30}")
    private int timeoutMinutes;

    /**
     * 每 2 分钟扫描一次超时未支付订单（兜底）。
     */
    @Scheduled(cron = "${app.mq.timeout-scan-cron:0 */2 * * * ?}")
    public void scanExpiredOrders() {
        try {
            List<Order> expired = orderService.listExpiredPendingOrders(timeoutMinutes);
            if (expired.isEmpty()) {
                return;
            }
            LOGGER.info("扫描到超时未支付订单 {} 笔，开始自动关单", expired.size());
            int closed = 0;
            for (Order order : expired) {
                boolean ok = orderMessageProcessor.cancelExpiredOrder(
                        order.getId(), order.getQuantity(), order.getProductId());
                if (ok) {
                    closed++;
                }
            }
            LOGGER.info("超时关单完成，成功关闭 {} 笔", closed);
        } catch (Exception e) {
            LOGGER.error("超时关单扫描异常：{}", e.getMessage(), e);
        }
    }
}
