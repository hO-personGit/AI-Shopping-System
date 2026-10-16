package org.example.springboot.reliable;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 本地消息表记录（可靠消息最终一致性）。
 *
 * <p>业务事务内写入业务数据 + 本表消息（同一事务），后台定时任务扫描 PENDING 消息投递，
 * 投递成功标记 SENT，失败按指数退避重试，超过最大次数标记 FAILED 供人工介入。
 */
@Data
@TableName("message_record")
public class LocalMessageRecord {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务幂等键（订单号等），唯一约束保证不重复投递。 */
    private String messageKey;

    /** 事件类型（ORDER_CREATE / ORDER_PAY_SUCCESS / ORDER_TIMEOUT_CLOSE / ORDER_CANCEL）。 */
    private String bizType;

    /** 消息体 JSON（OrderMessage 序列化）。 */
    private String payload;

    /** 状态：PENDING / SENT / FAILED。 */
    private String status;

    /** 已重试次数。 */
    private Integer retryCount;

    /** 下次重试时间（未到时间不投递）。 */
    private Timestamp nextRetryTime;

    private Timestamp createdAt;

    private Timestamp updatedAt;
}
