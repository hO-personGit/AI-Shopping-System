package org.example.springboot.enumClass;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机（Order State Machine）。
 *
 * <p>状态定义与数据库 order.status 一一对应：
 * <pre>
 * 0-待支付  1-已支付  2-已发货  3-已完成  4-已取消  5-退款中  6-已退款  7-退款失败
 * </pre>
 *
 * <p>合法流转（由业务规则固化，防止脏状态漂移）：
 * <pre>
 * 待支付(0) → 已支付(1)   [支付成功]
 * 待支付(0) → 已取消(4)   [超时未支付 / 用户主动取消 / 库存回滚]
 * 已支付(1) → 已发货(2)   [商家发货]
 * 已发货(2) → 已完成(3)   [用户确认收货 / 系统自动签收]
 * 已支付(1) → 退款中(5)   [用户申请退款]
 * 已发货(2) → 退款中(5)   [用户申请退款（已发货拦截逻辑由业务层控制）]
 * 退款中(5) → 已退款(6)   [商家同意退款]
 * 退款中(5) → 退款失败(7) [商家拒绝退款]
 * 已退款(6) → 已取消(4)   [退款后订单作废（业务可选）]
 * </pre>
 */
public enum OrderStatus {

    /** 0-待支付 */
    PENDING_PAYMENT(0, "待支付"),
    /** 1-已支付 */
    PAID(1, "已支付"),
    /** 2-已发货 */
    SHIPPED(2, "已发货"),
    /** 3-已完成 */
    COMPLETED(3, "已完成"),
    /** 4-已取消 */
    CANCELLED(4, "已取消"),
    /** 5-退款中 */
    REFUNDING(5, "退款中"),
    /** 6-已退款 */
    REFUNDED(6, "已退款"),
    /** 7-退款失败 */
    REFUND_FAILED(7, "退款失败");

    private final int code;
    private final String description;

    OrderStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 状态机：source → 允许到达的目标状态集合 */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(PENDING_PAYMENT, EnumSet.of(PAID, CANCELLED));
        TRANSITIONS.put(PAID, EnumSet.of(SHIPPED, REFUNDING));
        TRANSITIONS.put(SHIPPED, EnumSet.of(COMPLETED, REFUNDING));
        TRANSITIONS.put(COMPLETED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(REFUNDING, EnumSet.of(REFUNDED, REFUND_FAILED));
        TRANSITIONS.put(REFUNDED, EnumSet.of(CANCELLED));
        TRANSITIONS.put(REFUND_FAILED, EnumSet.noneOf(OrderStatus.class));
    }

    /**
     * 判断状态流转是否合法。
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return true 表示允许流转
     */
    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(OrderStatus.class)).contains(to);
    }

    /**
     * 判断状态流转是否合法（按状态码）。
     */
    public static boolean canTransition(int fromCode, int toCode) {
        OrderStatus from = fromCode(fromCode);
        OrderStatus to = fromCode(toCode);
        if (from == null || to == null) {
            return false;
        }
        return canTransition(from, to);
    }

    /**
     * 按状态码解析枚举，非法值返回 null。
     */
    public static OrderStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 校验并返回目标状态（用于状态变更接口，非法流转返回 null）。
     */
    public static OrderStatus validateTransition(Integer fromCode, Integer toCode) {
        OrderStatus from = fromCode(fromCode);
        OrderStatus to = fromCode(toCode);
        if (from == null || to == null) {
            return null;
        }
        return canTransition(from, to) ? to : null;
    }
}
