"""RAG 检索与生成质量评估 CLI（RAGAS 风格）。

用法：
    python -m app.evaluation.run_eval                # 默认跑全量评估
    python -m app.evaluation.run_eval --top-k 5 --output-dir eval_report
    python -m app.evaluation.run_eval --mode retrieval-only   # 仅检索指标（无需 LLM）

输出：eval_report/eval_report.json + eval_report/eval_report.md
"""
from __future__ import annotations

import argparse
import json
import time
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, List

from app.evaluation.dataset import get_eval_dataset
from app.evaluation.metrics import (answer_relevance_score, aggregate_metrics,
                                    context_precision, retrieval_metrics,
                                    source_overlap_score)


def _str_id(v: Any) -> str:
    return str(v)


def evaluate(top_k: int = 5, mode: str = "full") -> Dict[str, Any]:
    from app.services.ai_service import product_ai_service
    from app.services.vector_store import product_vector_store

    dataset = get_eval_dataset()
    results: List[Dict[str, Any]] = []
    errors: List[str] = []

    for item in dataset:
        query = item["query"]
        expected = [_str_id(x) for x in item.get("expected_ids", [])]
        row: Dict[str, Any] = {"query": query, "category": item.get("category", "")}

        # ---- 检索 ----
        try:
            hits = product_vector_store.hybrid_search(query, top_k)
        except Exception as exc:
            errors.append(f"{query} → 检索失败：{exc}")
            continue
        retrieved = [_str_id(h.get("id")) for h in hits]
        row.update(retrieval_metrics(expected, retrieved, top_k))
        row["contextPrecision"] = context_precision(expected, retrieved)
        row["retrievedIds"] = retrieved

        # ---- 生成（可选） ----
        if mode == "full":
            try:
                from app.schemas import GuideRequest
                result = product_ai_service.smart_guide(
                    GuideRequest(query=query, top_k=top_k))
                answer = result.answer or ""
                names = [r.name for r in result.recommendations[:top_k]]
            except Exception as exc:
                errors.append(f"{query} → 生成失败：{exc}")
                answer = ""
                names = [str(h.get("name") or "") for h in hits]
            row["answer"] = answer[:200]
            row["faithfulnessProxy"] = source_overlap_score(answer, names)
            row["answerRelevancyProxy"] = answer_relevance_score(query, answer)
        results.append(row)

    summary = {
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "topK": top_k,
        "mode": mode,
        "totalCases": len(dataset),
        "evaluatedCases": len(results),
        "errors": errors,
        "aggregate": aggregate_metrics(results),
        "cases": results,
    }
    return summary


def _write_report(summary: Dict[str, Any], output_dir: str) -> Path:
    out = Path(output_dir)
    out.mkdir(parents=True, exist_ok=True)
    (out / "eval_report.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")

    agg = summary["aggregate"]
    lines = [
        "# RAG 评估报告（RAGAS 风格）",
        "",
        f"- 生成时间：{summary['generatedAt']}",
        f"- 评估方式：{summary['mode']}（top_k={summary['topK']}）",
        f"- 样本数：{summary['evaluatedCases']}/{summary['totalCases']}",
        "",
        "## 聚合指标",
        "",
        "| 指标 | 值 |",
        "| --- | --- |",
    ]
    for k, v in agg.items():
        lines.append(f"| {k} | {v} |")
    if agg:
        lines.append("")
        lines.append("> 指标口径：recall@k 召回率 / precision@k 精确率 / mrr 平均倒数排名 / "
                     "ndcg@k 归一化折损累计增益 / faithfulnessProxy 忠实度代理 / "
                     "answerRelevancyProxy 相关性代理。")
    lines += ["", "## 分样本", "", "| 问题 | 类别 | recall@k | precision@k | mrr | ndcg@k | 忠实度 | 相关性 |", "| --- | --- | --- | --- | --- | --- | --- | --- |"]
    top_k = summary["topK"]
    recall_key = "recall@{0}".format(top_k)
    precision_key = "precision@{0}".format(top_k)
    ndcg_key = "ndcg@{0}".format(top_k)
    for c in summary["cases"]:
        q = c["query"]
        cat = c.get("category", "")
        row = "| {q} | {cat} | {recall} | {precision} | {mrr} | {ndcg} | {faith} | {rel} |".format(
            q=q, cat=cat,
            recall=c.get(recall_key, "-"),
            precision=c.get(precision_key, "-"),
            mrr=c.get("mrr", "-"),
            ndcg=c.get(ndcg_key, "-"),
            faith=c.get("faithfulnessProxy", "-"),
            rel=c.get("answerRelevancyProxy", "-"),
        )
        lines.append(row)
    if summary["errors"]:
        lines += ["", "## 异常", ""] + [f"- {e}" for e in summary["errors"]]
    report = out / "eval_report.md"
    report.write_text("\n".join(lines), encoding="utf-8")
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="RAG 质量评估（RAGAS 风格）")
    parser.add_argument("--top-k", type=int, default=5)
    parser.add_argument("--output-dir", default="eval_report")
    parser.add_argument("--mode", choices=["full", "retrieval-only"], default="full")
    args = parser.parse_args()

    start = time.time()
    summary = evaluate(top_k=args.top_k, mode=args.mode)
    report = _write_report(summary, args.output_dir)
    print(f"评估完成，耗时 {time.time() - start:.2f}s，报告已生成：{report}")
    print("聚合指标：", json.dumps(summary["aggregate"], ensure_ascii=False))


if __name__ == "__main__":
    main()
