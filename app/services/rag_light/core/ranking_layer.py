import math
import os
from typing import Any, Dict, List

import numpy as np

ALPHA = float(os.getenv("RANK_ALPHA", "0.6"))
TOPK_FALLBACK = int(os.getenv("RANK_TOPK_LIMIT", "50"))
DISPARITY_THRESHOLD = float(os.getenv("RANK_FREQ_DISPARITY_THRESHOLD", "5"))


def _dot(a: List[float], b: List[float]) -> float:
    if not a or not b:
        return 0.0
    if len(a) != len(b):
        return 0.0
    return float(np.dot(np.asarray(a, dtype=np.float32), np.asarray(b, dtype=np.float32)))


def _rank_rows(rows: List[Dict[str, Any]], query_vec: List[float], alpha: float) -> Dict[str, Any]:
    if not rows:
        return {
            "ranked": [],
            "stats": {
                "original_count": 0,
                "used_count": 0,
                "missing_embedding_count": 0,
                "scoring_mode": "none",
                "freq_mode": "none",
                "alpha": alpha,
            },
        }

    # 如果没有查询向量，直接使用所有行，按频率排序
    if not query_vec:
        ranked = []
        for r in rows:
            freq = r.get("freq", 0) or 0
            ranked.append(
                {
                    "id": r.get("id"),
                    "label": r.get("label"),
                    "text": r.get("text"),
                    "freq": freq,
                    "sim": 0.0,
                    "freq_norm": 1.0,
                    "score": float(freq),
                    "embedding": r.get("embedding"),
                }
            )
        ranked.sort(key=lambda x: x["score"], reverse=True)
        return {
            "ranked": ranked,
            "stats": {
                "original_count": len(rows),
                "used_count": len(rows),
                "missing_embedding_count": 0,
                "scoring_mode": "no_query_vector",
                "freq_mode": "direct",
                "alpha": alpha,
            },
        }

    usable = [r for r in rows if isinstance(r.get("embedding"), list)]
    missing_cnt = len(rows) - len(usable)

    if not usable:
        # 如果没有可用的嵌入向量，按频率排序
        ranked = []
        for r in rows:
            freq = r.get("freq", 0) or 0
            ranked.append(
                {
                    "id": r.get("id"),
                    "label": r.get("label"),
                    "text": r.get("text"),
                    "freq": freq,
                    "sim": 0.0,
                    "freq_norm": 1.0,
                    "score": float(freq),
                    "embedding": r.get("embedding"),
                }
            )
        ranked.sort(key=lambda x: x["score"], reverse=True)
        return {
            "ranked": ranked,
            "stats": {
                "original_count": len(rows),
                "used_count": len(rows),
                "missing_embedding_count": missing_cnt,
                "scoring_mode": "no_embeddings",
                "freq_mode": "direct",
                "alpha": alpha,
            },
        }

    freqs = [(r.get("freq", 0) or 0) for r in usable]
    max_freq = max(freqs)
    min_freq = min(freqs)

    ranked = []
    uniform = max_freq == min_freq
    if uniform:
        scoring_mode = "uniform_sim"
        freq_mode = "uniform"
    else:
        disparity = max_freq / max(min_freq, 1)
        if disparity > DISPARITY_THRESHOLD:
            freq_mode = "linear"
            scoring_mode = "alpha_mix"
        else:
            freq_mode = "log"
            scoring_mode = "alpha_mix"

    log_denom = math.log1p(max_freq) if not uniform and freq_mode == "log" else 1.0

    for r in usable:
        freq = r.get("freq", 0) or 0
        emb = r.get("embedding")
        sim = _dot(query_vec, emb) if query_vec and isinstance(emb, list) else 0.0

        if uniform:
            freq_norm = 1.0
            score = sim
        else:
            if freq_mode == "linear":
                freq_norm = freq / max_freq if max_freq > 0 else 0.0
            else:
                freq_norm = math.log1p(freq) / (log_denom + 1e-9)
            score = alpha * sim + (1 - alpha) * freq_norm

        ranked.append(
            {
                "id": r.get("id"),
                "label": r.get("label"),
                "text": r.get("text"),
                "freq": freq,
                "sim": sim,
                "freq_norm": freq_norm,
                "score": score,
                "embedding": emb,
            }
        )

    ranked.sort(key=lambda x: x["score"], reverse=True)

    stats = {
        "original_count": len(rows),
        "used_count": len(usable),
        "missing_embedding_count": missing_cnt,
        "scoring_mode": scoring_mode,
        "freq_mode": freq_mode,
        "alpha": alpha,
        "max_freq": max_freq,
        "min_freq": min_freq,
        "disparity": (max_freq / max(min_freq, 1)) if not uniform else 1.0,
    }
    return {"ranked": ranked, "stats": stats}


def rank_expansions(aggregate_output: Dict[str, Any]) -> Dict[str, Any]:
    query_vec = aggregate_output.get("query_vector")
    topk = aggregate_output.get("topk") or TOPK_FALLBACK
    expansions = aggregate_output.get("expansions", {})

    ranked_intents: Dict[str, Any] = {}
    for intent, block in expansions.items():
        rows = block.get("rows", [])
        rank_result = _rank_rows(rows, query_vec, ALPHA)
        ranked_rows = rank_result["ranked"]
        stats = rank_result["stats"]
        stats["query_vector_dim"] = len(query_vec) if isinstance(query_vec, list) else None

        ranked_intents[intent] = {
            "source": block.get("source"),
            "columns": ["id", "label", "text", "freq", "sim", "freq_norm", "score", "embedding"],
            "rows": ranked_rows[:topk],
            "stats": stats,
        }

    return {
        "question": aggregate_output.get("question"),
        "intents": aggregate_output.get("intents"),
        "topk": topk,
        "query_vector": query_vec,
        "ranked": ranked_intents,
        "meta": {
            "layer": "ranking",
            "previous_meta": aggregate_output.get("meta"),
            "scoring": "adaptive; uniform->sim; else score=alpha*sim+(1-alpha)*freq_norm",
            "alpha": ALPHA,
            "freq_disparity_threshold": DISPARITY_THRESHOLD,
        },
    }
