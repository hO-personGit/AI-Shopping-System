-- ============================================================
-- v4.0.0 本地消息表（可靠消息最终一致性）
-- 说明：业务事务内登记 PENDING 消息，定时任务扫描投递、失败指数退避重试
-- 执行：mysql -uroot -proot db_aps < v4.0.0_local_message_table.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS `message_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `message_key` VARCHAR(128) NOT NULL COMMENT '业务幂等键（订单号）',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '事件类型：ORDER_CREATE/ORDER_PAY_SUCCESS/ORDER_TIMEOUT_CLOSE/ORDER_CANCEL',
  `payload` TEXT COMMENT '消息体 JSON（OrderMessage 序列化）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/SENT/FAILED',
  `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
  `next_retry_time` DATETIME NOT NULL COMMENT '下次重试时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_key` (`message_key`),
  KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表（可靠消息最终一致性）';
