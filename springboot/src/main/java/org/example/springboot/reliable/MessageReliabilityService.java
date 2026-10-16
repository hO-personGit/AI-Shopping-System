package org.example.springboot.reliable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.example.springboot.mapper.OrderMapper;
import org.example.springboot.mq.OrderMessage;
import org.example.springboot.mq.OrderMessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息表服务：业务事务内登记消息 → 定时扫描投递 → 确认/重试。
 *
 * <p>解决「无 MQ 中间件 / MQ 不可用」时的可靠消息最终一致性：
 * <ul>
 *   <li>同事务写业务数据与消息记录，业务成功消息必不丢；</li>
 *   <li>定时任务拉取到期 PENDING 消息，交给 {@link OrderMessageProcessor} 幂等处理；</li>
 *   <li>成功标记 SENT，失败指数退避重试，超过 5 次标记 FAILED 供排查。</li>
 * </ul>
 */
@Service
public class MessageReliabilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageReliabilityService.class);

    /** 最大重试次数，超过后标记 FAILED。 */
    private static final int MAX_RETRY = 5;
    /** 基础重试间隔（秒），指数退避：5s、10s、20s、40s、80s。 */
    private static final long BASE_RETRY_SECONDS = 5;
    /** 定时扫描间隔（毫秒）。 */
    private static final long SCAN_INTERVAL_MS = 10_000L;
    /** 单次扫描上限。 */
    private static final int SCAN_LIMIT = 50;

    @Resource
    private LocalMessageRecordMapper messageRecordMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private OrderMessageProcessor orderMessageProcessor;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 下单事务入口：订单落库 + 登记 PENDING 消息（同一事务，保证不丢消息）。
     */
    @Transactional
    public void createOrderWithMessage(org.example.springboot.entity.Order order, OrderMessage message) {
        // 1. 业务数据落库（订单）
        orderMapper.insert(order);
        // 2. 本地消息记录（同事务）
        LocalMessageRecord record = new LocalMessageRecord();
        record.setMessageKey(order.getOrderNo());
        record.setBizType(message.getEventType());
        try {
            record.setPayload(objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            throw new IllegalStateException("消息序列化失败", e);
        }
        record.setStatus(LocalMessageRecord.STATUS_PENDING);
        record.setRetryCount(0);
        record.setNextRetryTime(Timestamp.valueOf(LocalDateTime.now()));
        messageRecordMapper.insert(record);
        LOGGER.info("本地消息已登记 key={} bizType={}", record.getMessageKey(), record.getBizType());
    }

    /**
     * 定时扫描并投递到期 PENDING 消息（每 10s）。
     */
    @Scheduled(fixedDelay = SCAN_INTERVAL_MS)
    public void deliverPendingMessages() {
        List<LocalMessageRecord> pending = messageRecordMapper.selectList(
                new LambdaQueryWrapper<LocalMessageRecord>()
                        .eq(LocalMessageRecord::getStatus, LocalMessageRecord.STATUS_PENDING)
                        .le(LocalMessageRecord::getNextRetryTime, Timestamp.valueOf(LocalDateTime.now()))
                        .last("LIMIT " + SCAN_LIMIT));
        if (pending.isEmpty()) {
            return;
        }
        LOGGER.info("本地消息投递扫描：{} 条待投递", pending.size());
        for (LocalMessageRecord record : pending) {
            deliver(record);
        }
    }

    /** 投递单条消息：成功 SENT，失败退避重试 / 超限 FAILED。 */
    private void deliver(LocalMessageRecord record) {
        try {
            OrderMessage message = objectMapper.readValue(record.getPayload(), OrderMessage.class);
            boolean ok = orderMessageProcessor.handle(message);
            if (ok) {
                markSent(record);
            } else {
                retry(record, "消息处理返回失败");
            }
        } catch (Exception e) {
            retry(record, "投递异常: " + e.getMessage());
        }
    }

    @Transactional
    public void markSent(LocalMessageRecord record) {
        record.setStatus(LocalMessageRecord.STATUS_SENT);
        messageRecordMapper.updateById(record);
        LOGGER.info("本地消息投递成功 key={} bizType={}", record.getMessageKey(), record.getBizType());
    }

    @Transactional
    public void retry(LocalMessageRecord record, String reason) {
        int nextCount = record.getRetryCount() + 1;
        record.setRetryCount(nextCount);
        if (nextCount > MAX_RETRY) {
            record.setStatus(LocalMessageRecord.STATUS_FAILED);
            LOGGER.error("本地消息投递失败且超过最大重试，标记 FAILED key={} reason={}",
                    record.getMessageKey(), reason);
        } else {
            long delaySeconds = BASE_RETRY_SECONDS * (1L << (nextCount - 1)); // 指数退避
            record.setNextRetryTime(Timestamp.valueOf(LocalDateTime.now().plusSeconds(delaySeconds)));
            LOGGER.warn("本地消息投递失败，第 {} 次重试（{}s 后）key={} reason={}",
                    nextCount, delaySeconds, record.getMessageKey(), reason);
        }
        messageRecordMapper.updateById(record);
    }
}
