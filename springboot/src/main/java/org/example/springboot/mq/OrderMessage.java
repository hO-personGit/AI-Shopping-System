package org.example.springboot.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单异步消息体。
 *
 * <p>通过 RabbitMQ 传递，JSON 序列化。携带订单创建所需快照与事件信息，
 * 消费者据此幂等执行「落库 / 改状态 / 回补库存」等操作。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件类型：ORDER_CREATE / ORDER_PAY_SUCCESS / ORDER_TIMEOUT_CLOSE / ORDER_CANCEL */
    private String eventType;

    /** 订单主键（创建成功后回填，支付/关单/取消事件使用） */
    private Long orderId;

    /** 业务订单号（幂等键） */
    private String orderNo;

    // ---------- 下单快照（ORDER_CREATE 使用） ----------
    private Long userId;
    private Long productId;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private String recvName;
    private String recvAddress;
    private String recvPhone;
    private String remark;

    /** 支付金额（ORDER_PAY_SUCCESS 使用） */
    private BigDecimal payAmount;

    /** 超时关单延迟（毫秒），由生产者设置 */
    private Long delayMillis;

    /** 重试次数（消费失败重试计数） */
    private Integer retryCount;

    /** 消息创建时间戳 */
    private Long timestamp;
}
