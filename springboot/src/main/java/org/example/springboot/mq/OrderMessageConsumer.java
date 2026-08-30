package org.example.springboot.mq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 订单消息消费者（RabbitMQ 监听）。
 *
 * <p>手动 ACK + 幂等处理：业务处理成功才 basicAck；失败重试（丢弃走死信队列，由重试策略决定）。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class OrderMessageConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderMessageConsumer.class);

    @Autowired
    private OrderMessageHandler handler;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDER_CREATE)
    public void onCreate(OrderMessage message, Channel channel, Message raw) {
        consume(message, channel, raw);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDER_PAY)
    public void onPay(OrderMessage message, Channel channel, Message raw) {
        consume(message, channel, raw);
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDER_CANCEL)
    public void onCancel(OrderMessage message, Channel channel, Message raw) {
        consume(message, channel, raw);
    }

    private void consume(OrderMessage message, Channel channel, Message raw) {
        long deliveryTag = raw.getMessageProperties().getDeliveryTag();
        boolean ok = handler.handle(message);
        try {
            if (ok) {
                channel.basicAck(deliveryTag, false);
                LOGGER.info("订单消息已确认 deliveryTag={} eventType={}", deliveryTag,
                        message == null ? null : message.getEventType());
            } else {
                // 处理失败：requeue=false，进入死信队列兜底，避免无限重试积压
                channel.basicReject(deliveryTag, false);
                LOGGER.warn("订单消息处理失败，拒绝入死信 deliveryTag={} eventType={}", deliveryTag,
                        message == null ? null : message.getEventType());
            }
        } catch (Exception e) {
            LOGGER.error("确认订单消息异常 deliveryTag={} err={}", deliveryTag, e.getMessage(), e);
        }
    }
}
