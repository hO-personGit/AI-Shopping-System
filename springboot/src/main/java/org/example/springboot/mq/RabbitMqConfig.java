package org.example.springboot.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：交换机 / 队列 / 绑定 + 延迟队列（TTL + 死信交换机 DLX）+ 消息可靠性。
 *
 * <p>设计要点（面试/架构）：
 * <ul>
 *   <li>削峰：下单请求 → Redis 预扣库存 → 发 MQ，由消费者异步落库，写请求被 MQ 削峰。</li>
 *   <li>解耦：订单创建 / 支付 / 关单事件解耦，各自消费者独立伸缩。</li>
 *   <li>延迟关单：通过「TTL + 死信交换机」实现延迟队列，订单超时自动进入取消队列。</li>
 *   <li>可靠性：Publisher Confirm + 消费者手动 ACK + 失败重试 + 死信兜底。</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "aps.order.exchange";
    public static final String ORDER_DLX = "aps.order.dlx";

    public static final String QUEUE_ORDER_CREATE = "aps.order.create.queue";
    public static final String QUEUE_ORDER_PAY = "aps.order.pay.queue";
    public static final String QUEUE_ORDER_CANCEL = "aps.order.cancel.queue";
    public static final String QUEUE_ORDER_DELAY = "aps.order.delay.queue";

    @Value("${app.mq.order-timeout-ms:1800000}")
    private long orderTimeoutMs;

    // ---------- 交换机 ----------

    /** 业务交换机：direct，路由键即事件 routing key */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    /** 死信交换机：延迟队列投递超时消息到取消队列 */
    @Bean
    public DirectExchange orderDlx() {
        return new DirectExchange(ORDER_DLX, true, false);
    }

    // ---------- 队列 ----------

    @Bean
    public Queue orderCreateQueue() {
        return new Queue(QUEUE_ORDER_CREATE, true);
    }

    @Bean
    public Queue orderPayQueue() {
        return new Queue(QUEUE_ORDER_PAY, true);
    }

    @Bean
    public Queue orderCancelQueue() {
        return new Queue(QUEUE_ORDER_CANCEL, true);
    }

    /** 延迟队列：消息存活 orderTimeoutMs 后经 DLX 进入取消队列 */
    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_DELAY)
                .ttl((int) orderTimeoutMs)
                .deadLetterExchange(ORDER_DLX)
                .deadLetterRoutingKey(OrderEventType.ORDER_TIMEOUT_CLOSE.getRoutingKey())
                .build();
    }

    // ---------- 绑定 ----------

    @Bean
    public Binding bindCreate(Queue orderCreateQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCreateQueue).to(orderExchange)
                .with(OrderEventType.ORDER_CREATE.getRoutingKey());
    }

    @Bean
    public Binding bindPay(Queue orderPayQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderPayQueue).to(orderExchange)
                .with(OrderEventType.ORDER_PAY_SUCCESS.getRoutingKey());
    }

    @Bean
    public Binding bindCancel(Queue orderCancelQueue, DirectExchange orderDlx) {
        return BindingBuilder.bind(orderCancelQueue).to(orderDlx)
                .with(OrderEventType.ORDER_TIMEOUT_CLOSE.getRoutingKey());
    }

    @Bean
    public Binding bindCancelDirect(Queue orderCancelQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCancelQueue).to(orderExchange)
                .with(OrderEventType.ORDER_CANCEL.getRoutingKey());
    }

    @Bean
    public Binding bindDelay(Queue orderDelayQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderDelayQueue).to(orderExchange)
                .with(OrderEventType.ORDER_TIMEOUT_CLOSE.getRoutingKey());
    }

    // ---------- 序列化与确认 ----------

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** 声明交换机/队列：应用启动时确保元数据存在 */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    /** 手动 ACK：业务处理成功才确认，失败则重回队列/进入死信 */
    @Bean
    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            MessageConverter jacksonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(10);
        return factory;
    }
}
