package org.example.springboot.mq;

/**
 * 订单消息事件类型。
 *
 * <p>围绕「下单 → 支付 → 发货 → 完成 / 取消 / 退款」的订单生命周期，定义异步事件：
 * <ul>
 *   <li>ORDER_CREATE：创建订单（削峰：高并发下单先预扣库存，消息落库）</li>
 *   <li>ORDER_PAY_SUCCESS：支付成功（状态 0→1，数据库扣库存、加销量）</li>
 *   <li>ORDER_TIMEOUT_CLOSE：超时未支付自动关单（延迟队列触发，回补库存）</li>
 *   <li>ORDER_CANCEL：取消订单（用户取消 / 业务回滚）</li>
 * </ul>
 */
public enum OrderEventType {

    /** 创建订单 */
    ORDER_CREATE("order.create"),
    /** 支付成功 */
    ORDER_PAY_SUCCESS("order.pay"),
    /** 超时未支付自动关闭 */
    ORDER_TIMEOUT_CLOSE("order.timeout"),
    /** 取消订单 */
    ORDER_CANCEL("order.cancel");

    private final String routingKey;

    OrderEventType(String routingKey) {
        this.routingKey = routingKey;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public static OrderEventType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (OrderEventType type : values()) {
            if (type.name().equals(name) || type.routingKey.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
