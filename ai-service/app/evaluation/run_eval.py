"""RAG 检索与生成质量评估 CLI（RAGAS 风格）。

用法：
    python -m app.evaluation.run_eval                          # 默认全量评估（hybrid + lexical 精排）
    python -m app.evaluation.run_eval --top-k 5 --output-dir eval_report
    python -m app.evaluation.run_eval --mode retrieval-only    # 仅检索指标（无需 LLM）
    python -m app.evaluation.run_eval --rerank none            # 关闭精排评估
    python -m app.evaluation.run_eval --compare                # 精排开/关对比（recall/mrr/ndcg 增益）

输出：eval_report/eval_report.json + eval_report/eval_report.md
对比模式额外输出 eval_report/eval_compare.md
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


def _run_dataset(top_k: int, mode: str, rerank_mode: str) -> Dict[str, Any]:
    from app.services.ai_service import product_ai_service
    from app.services.reranker import reranker
    from app.services.vector_store import product_vector_store

    # 临时切换精排模式（评估结束后恢复）
    old_rerank = reranker.mode
    reranker.mode = rerank_mode
    try:
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

        return {
            "generatedAt": datetime.now().isoformat(timespec="seconds"),
            "topK": top_k,
            "mode": mode,
            "rerankMode": rerank_mode,
            "totalCases": len(dataset),
            "evaluatedCases": len(results),
            "errors": errors,
            "aggregate": aggregate_metrics(results),
            "cases": results,
        }
    finally:
        reranker.mode = old_rerank


def evaluate(top_k: int = 5, mode: str = "full",
             rerank_mode: str = "lexical", compare: bool = False) -> Dict[str, Any]:
    """执行评估；compare=True 时同时跑 lexical 与 none 两组并附加对比结论。"""
    summary = _run_dataset(top_k, mode, rerank_mode)
    if not compare:
        return summary

    base = _run_dataset(top_k, mode, "none")
    summary["compare"] = _compare_summary(base, summary, top_k)
    return summary


def _compare_summary(base: Dict[str, Any], reranked: Dict[str, Any], top_k: int) -> Dict[str, Any]:
    """对比「无精排 / 精排」两组聚合指标，计算增益。"""
    b = base.get("aggregate", {})
    r = reranked.get("aggregate", {})
    recall_key = f"recall@{top_k}"
    ndcg_key = f"ndcg@{top_k}"
    rows = []
    for key in (recall_key, f"precision@{top_k}", f"hitRate@{top_k}", "mrr", ndcg_key):
        bv, rv = b.get(key, 0), r.get(key, 0)
        delta = (rv - bv) / bv * 100 if bv else (0.0 if rv == bv else float("inf"))
        rows.append({"metric": key, "baseline(none)": bv, "rerank(lexical)": rv,
                     "delta%": round(delta, 2)})
    return {"baseline": base["aggregate"], "reranked": reranked["aggregate"], "rows": rows}


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
        f"- 评估方式：{summary['mode']}（top_k={summary['topK']}，rerank={summary.get('rerankMode', 'lexical')}）",
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
        lines.append("> 指标口径：recall@k 召回率 / precision@k 精确率 / hitRate@k 命中率 / "
                     "mrr 平均倒数排名 / ndcg@k 归一化折损累计增益 / faithfulnessProxy 忠实度代理 / "
                     "answerRelevancyProxy 相关性代理。")

    if summary.get("compare"):
        lines += ["", "## 精排对比（lexical vs none）", "",
                  "| 指标 | 无精排(基线) | 精排后 | 增益% |", "| --- | --- | --- | --- |"]
        for row in summary["compare"]["rows"]:
            delta = row["delta%"]
            delta_str = "inf" if delta == float("inf") else f"{delta:.2f}"
            lines.append(f"| {row['metric']} | {row['baseline(none)']} | {row['rerank(lexical)']} | {delta_str} |")

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
    parser.add_argument("--rerank", choices=["none", "lexical", "api"], default="lexical")
    parser.add_argument("--compare", action="store_true", help="精排开/关对比评估")
    args = parser.parse_args()

    start = time.time()
    summary = evaluate(top_k=args.top_k, mode=args.mode,
                       rerank_mode=args.rerank, compare=args.compare)
    report = _write_report(summary, args.output_dir)
    print(f"评估完成，耗时 {time.time() - start:.2f}s，报告已生成：{report}")
    print("聚合指标：", json.dumps(summary["aggregate"], ensure_ascii=False))
    if summary.get("compare"):
        print("精排对比：")
        for row in summary["compare"]["rows"]:
            delta = "inf" if row["delta%"] == float("inf") else f"{row['delta%']}%"
            print(f"  {row['metric']}: {row['baseline(none)']} → {row['rerank(lexical)']} ({delta})")


if __name__ == "__main__":
    main()
