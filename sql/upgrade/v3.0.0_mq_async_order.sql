-- ============================================================
-- v3.0.0 · MQ 异步下单链路 数据库变更
-- 新增 order_no（业务订单号/幂等键）
-- 执行方式：mysql -uroot -p db_aps < v3.0.0_mq_async_order.sql
-- ============================================================

ALTER TABLE `order`
    ADD COLUMN `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci
        NULL DEFAULT NULL COMMENT '业务订单号（MQ异步下单幂等键）' AFTER `id`,
    ADD UNIQUE KEY `uk_order_no` (`order_no`) USING BTREE COMMENT '订单号唯一索引（幂等）';

-- 说明：
-- 1) 该变更仅在启用 app.mq.enabled=true 时必须执行；
-- 2) 不启用 MQ 异步下单（默认同步）时可不执行，代码对缺失列兼容（不设置 order_no）。
