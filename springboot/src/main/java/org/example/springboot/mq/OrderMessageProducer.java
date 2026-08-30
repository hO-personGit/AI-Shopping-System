package org.example.springboot.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 订单消息生产者。
 *
 * <p>发送订单事件到 RabbitMQ；当 MQ 未启用或连接不可用时，自动降级为本地同步分发
 * （调用 {@link OrderMessageHandler}），保证演示环境与无 MQ 场景可运行。
 *
 * <p>可靠性：开启 Publisher Confirm（application.properties 配置 publisher-confirm-type=correlated）。
 */
@Component
public class OrderMessageProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessageProducer.class);

    private final boolean mqEnabled;
    private final RabbitTemplate rabbitTemplate;
    private final OrderMessageHandler localHandler;

    public OrderMessageProducer(
            @Value("${app.mq.enabled:false}") boolean mqEnabled,
            @Autowired(required = false) RabbitTemplate rabbitTemplate,
            @Autowired(required = false) OrderMessageHandler localHandler) {
        this.mqEnabled = mqEnabled;
        this.rabbitTemplate = rabbitTemplate;
        this.localHandler = localHandler;
    }

    /**
     * 发布订单事件。
     *
     * @param message   订单消息
     * @param isDelay   true 表示走延迟队列（超时关单），false 走业务交换机
     */
    public void publish(OrderMessage message, boolean isDelay) {
        message.setTimestamp(System.currentTimeMillis());
        if (message.getRetryCount() == null) {
            message.setRetryCount(0);
        }
        OrderEventType type = OrderEventType.fromName(message.getEventType());
        if (type == null) {
            LOGGER.warn("非法事件类型，丢弃消息: {}", message.getEventType());
            return;
        }

        if (mqEnabled && rabbitTemplate != null) {
            try {
                String routingKey = type.getRoutingKey();
                // 延迟关单：投递到延迟队列（TTL 到期后经 DLX 进入取消队列）
                if (isDelay && OrderEventType.ORDER_TIMEOUT_CLOSE.equals(type)) {
                    rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE,
                            OrderEventType.ORDER_TIMEOUT_CLOSE.getRoutingKey(), message);
                } else {
                    rabbitTemplate.convertAndSend(RabbitMqConfig.ORDER_EXCHANGE, routingKey, message);
                }
                LOGGER.info("订单消息已发送 eventType={} orderNo={} routingKey={}", type, message.getOrderNo(), routingKey);
                return;
            } catch (AmqpException e) {
                LOGGER.warn("RabbitMQ 发送失败，降级本地分发 eventType={} err={}", type, e.getMessage());
            }
        }
        // 本地降级：同步调用处理器（保证链路可用）
        if (localHandler != null) {
            try {
                localHandler.handle(message);
                LOGGER.info("订单消息本地降级处理完成 eventType={} orderNo={}", type, message.getOrderNo());
            } catch (Exception e) {
                LOGGER.error("订单消息本地降级处理失败 eventType={} err={}", type, e.getMessage(), e);
            }
        } else {
            LOGGER.warn("无可用消息通道，订单消息被丢弃 eventType={}", type);
        }
    }
}
