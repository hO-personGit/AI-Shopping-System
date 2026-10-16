package org.example.springboot.mq;

/**
 * 订单消息处理器：对订单事件执行幂等的业务操作。
 *
 * <p>由 {@link OrderMessageConsumer}（RabbitMQ 监听）与 {@link OrderMessageProducer}
 * （RabbitMQ 不可用时的本地降级）共同复用，保证「异步 MQ 消费」与「同步兜底」两条链路
 * 执行完全一致的业务逻辑。
 */
public interface OrderMessageHandler {

    /**
     * 处理订单消息。
     *
     * @param message 订单消息
     * @return true 表示处理成功（可确认消息 / 不重试）
     */
    boolean handle(OrderMessage message);
}
