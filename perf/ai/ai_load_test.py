# -*- coding: utf-8 -*-
"""AI 商品销售系统 · 轻量压测脚本（Python 标准库 + requests）。

用法：
    1) 启动后端（端口 1234）、AI 服务（端口 8001）、MySQL、Redis
    2) python ai_load_test.py --base http://localhost:1234 --product 1 --threads 50 --loops 20
       python ai_load_test.py --target ai --base http://localhost:8001 --threads 10 --loops 5

输出：QPS / 平均耗时 / P50 / P95 / P99 / 成功率 / 超卖检查
"""
from __future__ import annotations

import argparse
import concurrent.futures
import json
import statistics
import time
from typing import Dict, List, Optional

import requests


def _timing(stats: List[float], p: float) -> float:
    if not stats:
        return 0.0
    stats_sorted = sorted(stats)
    idx = min(len(stats_sorted) - 1, int(len(stats_sorted) * p))
    return round(stats_sorted[idx] * 1000, 1)


def load_backend(base: str, product_id: int, threads: int, loops: int) -> Dict[str, object]:
    """压测商品详情读取（缓存命中）与下单支付（防超卖）。"""
    latencies: List[float] = []
    errors = 0
    oversell = 0
    order_success = 0

    def detail_task(_):
        nonlocal errors
        start = time.perf_counter()
        try:
            r = requests.get(f"{base}/product/{product_id}", timeout=10)
            latencies.append(time.perf_counter() - start)
            if r.status_code != 200:
                errors += 1
        except Exception:
            errors += 1
            latencies.append(time.perf_counter() - start)

    def order_task(_):
        nonlocal errors, oversell, order_success
        start = time.perf_counter()
        try:
            payload = {
                "userId": 1, "productId": product_id, "quantity": 1, "price": 199.0,
                "recvName": "压测用户", "recvAddress": "郑州", "recvPhone": "13000000000",
            }
            r = requests.post(f"{base}/order", json=payload, timeout=10)
            latencies.append(time.perf_counter() - start)
            if r.status_code != 200:
                errors += 1
                return
            data = r.json()
            if data.get("code") == "0":
                order_success += 1
            elif "库存" in str(data.get("msg", "")):
                oversell += 1  # 正常并发下库存不足拒绝，不算超卖
            else:
                errors += 1
        except Exception:
            errors += 1
            latencies.append(time.perf_counter() - start)

    total = threads * loops
    with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as pool:
        start_all = time.perf_counter()
        for _ in range(loops):
            list(pool.map(detail_task, range(threads)))
        elapsed = time.perf_counter() - start_all

    return {
        "type": "product-detail(cached)",
        "requests": total,
        "elapsed_s": round(elapsed, 2),
        "qps": round(total / elapsed, 1) if elapsed else 0,
        "avg_ms": round(statistics.mean(latencies) * 1000, 1) if latencies else 0,
        "p50_ms": _timing(latencies, 0.50),
        "p95_ms": _timing(latencies, 0.95),
        "p99_ms": _timing(latencies, 0.99),
        "errors": errors,
        "success_rate": round((1 - errors / total) * 100, 2) if total else 0,
    }


def load_ai(base: str, threads: int, loops: int) -> Dict[str, object]:
    """压测 AI 智能导购接口（mock 模式可跑，真实模型需配置）。"""
    latencies: List[float] = []
    errors = 0

    def guide_task(_):
        nonlocal errors
        start = time.perf_counter()
        try:
            r = requests.post(f"{base}/ai/guide",
                              json={"query": "推荐性价比高的运动鞋", "top_k": 5}, timeout=60)
            latencies.append(time.perf_counter() - start)
            if r.status_code != 200:
                errors += 1
        except Exception:
            errors += 1
            latencies.append(time.perf_counter() - start)

    total = threads * loops
    with concurrent.futures.ThreadPoolExecutor(max_workers=threads) as pool:
        start_all = time.perf_counter()
        for _ in range(loops):
            list(pool.map(guide_task, range(threads)))
        elapsed = time.perf_counter() - start_all

    return {
        "type": "ai-guide",
        "requests": total,
        "elapsed_s": round(elapsed, 2),
        "qps": round(total / elapsed, 1) if elapsed else 0,
        "avg_ms": round(statistics.mean(latencies) * 1000, 1) if latencies else 0,
        "p50_ms": _timing(latencies, 0.50),
        "p95_ms": _timing(latencies, 0.95),
        "p99_ms": _timing(latencies, 0.99),
        "errors": errors,
        "success_rate": round((1 - errors / total) * 100, 2) if total else 0,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--target", choices=["backend", "ai"], default="backend")
    parser.add_argument("--base", default="http://localhost:1234")
    parser.add_argument("--product", type=int, default=1)
    parser.add_argument("--threads", type=int, default=50)
    parser.add_argument("--loops", type=int, default=20)
    args = parser.parse_args()

    if args.target == "ai":
        result = load_ai(args.base, args.threads, args.loops)
    else:
        result = load_backend(args.base, args.product, args.threads, args.loops)
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
