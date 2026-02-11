import json
import os
from typing import Any, Dict, List, Tuple

import requests

LLM_PROVIDER = os.getenv("LLM_PROVIDER", "openai").lower()
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-3.5-turbo").strip()
LLM_API_BASE = os.getenv("LLM_API_BASE", "https://api.openai.com").strip()
LLM_API_KEY = os.getenv("LLM_API_KEY", "").strip()
LLM_TIMEOUT = float(os.getenv("LLM_TIMEOUT", "60"))

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))

USE_LLM = os.getenv("USE_LLM", "true").lower() == "true"
MAX_ITEMS = int(os.getenv("MAX_ITEMS", "10"))

OUTCOME_ALIAS = {
    "DE": "死亡",
    "LT": "危及生命",
    "HO": "住院或延长住院",
    "DS": "致残/功能受限",
    "CA": "先天异常",
    "RI": "需干预以避免永久性伤害",
    "OT": "其他重要医学事件",
}


def _fmt_intent(intent: str) -> str:
    mapping = {
        "Drug": "药物",
        "Reaction": "不良反应",
        "Indication": "适应症",
        "Outcome": "患者结局",
    }
    return mapping.get(intent, intent)


def _normalize_text(intent: str, text: str) -> Tuple[str, str]:
    t = (text or "").strip()
    if intent == "Outcome":
        t_up = t.upper()
        if t_up in OUTCOME_ALIAS:
            return t_up, OUTCOME_ALIAS[t_up]
    return t, ""


def _dedup_and_map(items: List[Dict[str, Any]], intent: str, limit: int) -> List[Dict[str, Any]]:
    buckets: Dict[str, Dict[str, Any]] = {}
    for r in items:
        txt = (r.get("text") or "").strip()
        if not txt:
            continue
        key_en, cn_hint = _normalize_text(intent, txt)
        score = float(r.get("score") or 0.0)
        freq = r.get("freq")
        if key_en not in buckets:
            buckets[key_en] = {
                "text_en": key_en,
                "text_cn_hint": cn_hint,
                "count": 1,
                "best_score": score,
                "freq": freq,
            }
        else:
            buckets[key_en]["count"] += 1
            if score > buckets[key_en]["best_score"]:
                buckets[key_en]["best_score"] = score
                buckets[key_en]["freq"] = freq
            if cn_hint and not buckets[key_en]["text_cn_hint"]:
                buckets[key_en]["text_cn_hint"] = cn_hint
    merged = list(buckets.values())
    merged.sort(key=lambda x: (x["count"], x["best_score"]), reverse=True)
    return merged[:limit]


def _local_compose_answer(per_intent: Dict[str, List[Dict[str, Any]]], scoring_note: str) -> str:
    parts: List[str] = []
    for intent, rows in per_intent.items():
        title = _fmt_intent(intent)
        lines = [f"{title}{scoring_note}："]
        for i, r in enumerate(rows, 1):
            en = r["text_en"]
            cn = r.get("text_cn_hint")
            cnt = r.get("count", 1)
            label = f"{cn}（{en}）" if cn else en
            if cnt > 1:
                lines.append(f"{i}. {label}（出现{cnt}次）")
            else:
                lines.append(f"{i}. {label}")
        parts.append("\n".join(lines))
    return "\n\n".join(parts)


def _openai_chat(messages: List[Dict[str, str]]) -> str:
    if not LLM_API_KEY:
        raise RuntimeError("LLM_API_KEY 未配置")
    url = LLM_API_BASE.rstrip("/") + "/v1/chat/completions"
    headers = {"Authorization": f"Bearer {LLM_API_KEY}", "Content-Type": "application/json"}
    payload = {"model": LLM_MODEL, "messages": messages, "temperature": 0.2}
    resp = requests.post(url, json=payload, headers=headers, timeout=LLM_TIMEOUT)
    resp.raise_for_status()
    data = resp.json()
    return data["choices"][0]["message"]["content"].strip()


def _ollama_chat(messages: List[Dict[str, str]]) -> str:
    payload = {"model": OLLAMA_MODEL, "messages": messages, "stream": False}
    resp = requests.post(
        f"{OLLAMA_BASE_URL}/api/chat",
        json=payload,
        timeout=OLLAMA_TIMEOUT,
    )
    resp.raise_for_status()
    data = resp.json()
    content = (data.get("message") or {}).get("content", "")
    return content.strip()


def _llm_refine(question: str,
                per_intent: Dict[str, List[Dict[str, Any]]],
                scoring_mode_by_intent: Dict[str, str]) -> str:
    lists_text: List[str] = []
    for intent, rows in per_intent.items():
        zh_title = _fmt_intent(intent)
        scoring_note = "仅语义相关排序" if scoring_mode_by_intent.get(intent) == "uniform_sim" else "综合语义与频次"
        lists_text.append(f"{zh_title}（{scoring_note}）：")
        for i, r in enumerate(rows, 1):
            en = r["text_en"]
            cn_hint = r.get("text_cn_hint")
            cnt = r.get("count", 1)
            item = f"{i}. {cn_hint}（{en}）" if cn_hint else f"{i}. {en}"
            if cnt > 1:
                item += f"（出现{cnt}次）"
            lists_text.append(item)
    context = "\n".join(lists_text)

    prompt = f"""请用简体中文回答下面的问题，并基于给定要点进行归纳：
要求：
1. 不编造或扩展未提供的信息；
2. 合并相近概念，不机械逐条照搬；
3. 药物名可保留英文，若常见可加中文；
4. 使用分点或分段，简洁专业；
5. 不要原样复制"原始要点"列表；
6. 如果要点很杂，可按类别归组。
7. 只输出回答内容。

用户问题：{question}

原始要点：
{context}

请直接输出优化后的中文回答："""

    messages = [
        {"role": "system", "content": "你是医学药物安全知识图谱助手，回答需准确、凝练。"},
        {"role": "user", "content": prompt},
    ]

    if LLM_PROVIDER == "ollama":
        return _ollama_chat(messages)
    return _openai_chat(messages)


def generate_answer(ranking_output: Dict[str, Any]) -> Dict[str, Any]:
    question = ranking_output.get("question", "")
    ranked = ranking_output.get("ranked", {})
    intents = ranking_output.get("intents") or []
    topk = ranking_output.get("topk", 10)

    per_intent_for_llm: Dict[str, List[Dict[str, Any]]] = {}
    scoring_mode_by_intent: Dict[str, str] = {}

    for intent in intents:
        block = ranked.get(intent) or {}
        rows = block.get("rows", [])
        stats = block.get("stats", {}) or {}
        scoring_mode = stats.get("scoring_mode", "unknown")
        scoring_mode_by_intent[intent] = scoring_mode
        dedup_rows = _dedup_and_map(rows[:topk], intent, MAX_ITEMS)
        per_intent_for_llm[intent] = dedup_rows

    if USE_LLM:
        try:
            llm_text = _llm_refine(question, per_intent_for_llm, scoring_mode_by_intent)
            ok = True
            err = ""
        except Exception as exc:  # noqa: BLE001
            ok = False
            err = str(exc)
            first_mode = next(iter(scoring_mode_by_intent.values()), "uniform_sim")
            note = "（仅语义相关排序，LLM失败回退枚举）" if first_mode == "uniform_sim" else "（综合语义与频次，LLM失败回退枚举）"
            llm_text = _local_compose_answer(per_intent_for_llm, note)
    else:
        ok = False
        err = "LLM disabled"
        first_mode = next(iter(scoring_mode_by_intent.values()), "uniform_sim")
        note = "（仅语义相关排序，本地枚举）" if first_mode == "uniform_sim" else "（综合语义与频次，本地枚举）"
        llm_text = _local_compose_answer(per_intent_for_llm, note)

    return {
        "question": question,
        "answer": llm_text,
        "success": True,
        "error": "" if ok else err,
        "meta": {
            "layer": "answer",
            "llm_model": LLM_MODEL,
            "llm_used": USE_LLM,
            "llm_success": ok,
            "llm_error": None if ok else err,
        },
    }
