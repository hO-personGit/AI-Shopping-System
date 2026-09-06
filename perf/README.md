# 性能压测量化

本目录为「AI 智能商品销售系统」的性能压测资产，用于把缓存、并发、AI 链路的优化**量化到简历/报告中**。

## 一、指标口径

| 指标 | 口径 |
| --- | --- |
| QPS | 每秒完成请求数 = 总请求数 / 压测时长 |
| 平均耗时 | 所有请求响应耗时均值（ms） |
| P50 / P95 / P99 | 响应耗时百分位（ms），反映大多数用户与极端场景体验 |
| 成功率 | 2xx / 总请求（下单/支付按业务码 0 判定） |
| 超卖数 | 并发下单支付后，实际扣减库存是否超过可售库存 |
| 缓存命中率 | 命中 Caffeine 一级缓存 / Redis 二级缓存的请求占比（可通过日志或 Redis 统计） |

> 压测前建议先调用一次接口完成缓存预热；对比「缓存开/关」两组数据，量化缓存收益。

## 二、JMeter 压测（推荐）

前置：启动后端（localhost:1234）+ MySQL + Redis；启用缓存防护。

```bash
# 启动 JMeter 并执行测试计划
jmeter -n -t perf/jmeter/backend-api.jmx -l result.jtl -e -o report/

# 查看 report/index.html 聚合报告
```

测试计划覆盖：
- 商品详情读取（缓存命中）：50 并发 × 100 次
- 商品列表读取（分页缓存）：50 并发 × 100 次
- 下单+支付并发（防超卖）：30 并发 × 20 次

## 三、Python 轻量压测

不依赖 JMeter，适合快速冒烟（接口路径已含 `/api` 前缀）：

```bash
# 后端：商品详情缓存命中压测
python perf/ai/ai_load_test.py --target detail --base http://localhost:1234 --product 1 --threads 50 --loops 20

# 后端：下单接口压测（观察成功率/库存拒绝）
python perf/ai/ai_load_test.py --target order --base http://localhost:1234 --product 1 --threads 30 --loops 20

# AI 智能导购接口压测
python perf/ai/ai_load_test.py --target ai --base http://localhost:8001 --threads 10 --loops 5
```

输出示例：

```json
{
  "type": "product-detail(cached)",
  "requests": 1000,
  "elapsed_s": 3.2,
  "qps": 312.5,
  "avg_ms": 158.0,
  "p50_ms": 120.0,
  "p95_ms": 260.0,
  "p99_ms": 420.0,
  "errors": 0,
  "success_rate": 100.0
}
```

## 四、结果记录（v4.0.0 实测，2026-09-06）

> 环境：Win 单机，MySQL 9.5 本机，后端 1234，AI mock 8001；商品详情/下单为 Caffeine 缓存+同步落库基线，Redis/MQ 未部署（异步链路为 v3 设计，需 RabbitMQ 环境复测）。

| 场景 | 方案 | 请求数 | QPS | P50(ms) | P95(ms) | P99(ms) | 成功率 | 超卖 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 商品详情读取 | Caffeine 一级缓存 | 1000 | 1279.8 | 26.6 | 33.9 | 35.7 | 100% | - |
| 下单（同步落库） | 同步兜底 | 600 | 1050.9 | 22.6 | 28.7 | 30.9 | 100% | 0 |
| AI 智能导购 | mock + FAISS | 50 | 109.9 | 17.3 | 369.3 | 369.7 | 100% | - |

> 说明：AI 导购 P95/P99 偏高来自并发下首次索引加载；真实 LLM 链路需接入模型后复测。

## 五、结果记录模板（填入简历/报告）

| 场景 | 方案 | QPS | P99(ms) | 成功率 | 超卖 |
| --- | --- | --- | --- | --- | --- |
| 商品详情读取 | 未缓存（直连 DB） |  |  |  | - |
| 商品详情读取 | Caffeine 一级缓存 |  |  |  | - |
| 商品详情读取 | Caffeine + Redis 两级缓存 + 三防 |  |  |  | - |
| 下单支付 | 同步（Redis 锁防超卖） |  |  |  | 0 |
| 下单支付 | MQ 异步下单（预扣+削峰） |  |  |  | 0 |

> 验收标准（自检）：并发 1000 单下单支付无超卖；缓存命中后 P99 下降 50%+；MQ 异步下单接口响应 < 200ms。
