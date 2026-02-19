import json
import os
from typing import Any, Dict, List, Tuple

import requests

LLM_PROVIDER = os.getenv("LLM_PROVIDER", "openai").lower()
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-3.5-turbo").strip()
LLM_API_BASE = os.getenv("LLM_API_BASE", "https://api.openai.com").strip()
LLM_API_KEY = (
    os.getenv("LLM_API_KEY", "")
    or os.getenv("OPENAI_API_KEY", "")
    or os.getenv("DASHSCOPE_API_KEY", "")
).strip()
LLM_CONNECT_TIMEOUT = float(os.getenv("LLM_CONNECT_TIMEOUT", "10"))
LLM_TIMEOUT = float(os.getenv("LLM_TIMEOUT", "60"))

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
# 兼容：历史上 embedding 与 chat 共用 OLLAMA_MODEL。
# 现在建议拆分为 OLLAMA_CHAT_MODEL / OLLAMA_EMBED_MODEL，避免误用。
OLLAMA_CHAT_MODEL = os.getenv("OLLAMA_CHAT_MODEL") or os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))

USE_LLM = os.getenv("USE_LLM", "true").lower() == "true"
MAX_ITEMS = int(os.getenv("MAX_ITEMS", "10"))

ANSWER_LAYER_VERSION = "v3_split_events_fix_text_field_2026-02-18"

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
        freq_val = None
        if isinstance(freq, (int, float)):
            try:
                freq_val = int(freq)
            except Exception:
                freq_val = None
        if key_en not in buckets:
            buckets[key_en] = {
                "text_en": key_en,
                "text_cn_hint": cn_hint,
                "count": 1,
                "best_score": score,
                "freq": freq_val,
            }
        else:
            buckets[key_en]["count"] += 1
            if score > buckets[key_en]["best_score"]:
                buckets[key_en]["best_score"] = score
            if isinstance(freq_val, int):
                prev = buckets[key_en].get("freq")
                if not isinstance(prev, int) or freq_val > prev:
                    buckets[key_en]["freq"] = freq_val
            if cn_hint and not buckets[key_en]["text_cn_hint"]:
                buckets[key_en]["text_cn_hint"] = cn_hint
    merged = list(buckets.values())
    merged.sort(key=lambda x: (x.get("freq") is not None, x.get("freq") or 0, x["best_score"]), reverse=True)
    return merged[:limit]


def _split_reaction_rows(rows: List[Dict[str, Any]]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    """Separate likely adverse reactions from other report events.

    FAERS/ICSR exports can include terms that are not true adverse reactions
    (e.g., medication errors, product quality issues, diagnoses like COVID-19).
    We keep them, but move them into a separate bucket to avoid misleading answers.
    """

    adverse: List[Dict[str, Any]] = []
    other: List[Dict[str, Any]] = []

    other_keywords = (
        "dose omission",
        "medication error",
        "product quality",
        "product use",
        "off label",
        "wrong",
        "maladministration",
        "overdose",
        "underdose",
        "incorrect",
        "accidental",
        "device",
    )
    disease_keywords = (
        "covid",
        "sars-cov",
        "influenza",
        "pneumonia",
    )

    for r in rows:
        text = (r.get("text") or r.get("text_en") or "").strip()
        t = text.lower()
        if not t:
            continue

        is_other = any(k in t for k in other_keywords) or any(k in t for k in disease_keywords)
        if is_other:
            other.append(r)
        else:
            adverse.append(r)

    return adverse, other


def _local_compose_answer(per_intent: Dict[str, List[Dict[str, Any]]], scoring_note: str) -> str:
    parts: List[str] = []
    for intent, rows in per_intent.items():
        if intent == "Reaction":
            adverse, other = _split_reaction_rows(rows)
            title = _fmt_intent(intent)
            lines = [f"{title}{scoring_note}："]
            for i, r in enumerate(adverse, 1):
                en = r["text_en"]
                cn = r.get("text_cn_hint")
                freq = r.get("freq")
                cnt = r.get("count", 1)
                label = f"{cn}（{en}）" if cn else en
                if isinstance(freq, int) and freq > 0:
                    lines.append(f"{i}. {label}（freq={freq}）")
                elif cnt > 1:
                    lines.append(f"{i}. {label}（出现{cnt}次）")
                else:
                    lines.append(f"{i}. {label}")

            if other:
                lines.append("\n其他报告事件（不等同于不良反应，仅供参考）：")
                for j, r in enumerate(other, 1):
                    en = r["text_en"]
                    cn = r.get("text_cn_hint")
                    freq = r.get("freq")
                    label = f"{cn}（{en}）" if cn else en
                    if isinstance(freq, int) and freq > 0:
                        lines.append(f"- {label}（freq={freq}）")
                    else:
                        lines.append(f"- {label}")
            parts.append("\n".join(lines))
        else:
            title = _fmt_intent(intent)
            lines = [f"{title}{scoring_note}："]
            for i, r in enumerate(rows, 1):
                en = r["text_en"]
                cn = r.get("text_cn_hint")
                freq = r.get("freq")
                cnt = r.get("count", 1)
                label = f"{cn}（{en}）" if cn else en
                if isinstance(freq, int) and freq > 0:
                    lines.append(f"{i}. {label}（freq={freq}）")
                elif cnt > 1:
                    lines.append(f"{i}. {label}（出现{cnt}次）")
                else:
                    lines.append(f"{i}. {label}")
            parts.append("\n".join(lines))
    return "\n\n".join(parts)


def _openai_chat(messages: List[Dict[str, str]]) -> str:
    if not LLM_API_KEY:
        raise RuntimeError("LLM_API_KEY 未配置")

    base = LLM_API_BASE.strip().rstrip("/")
    if base.endswith("/v1"):
        base = base[: -len("/v1")]

    url = base + "/v1/chat/completions"
    headers = {"Authorization": f"Bearer {LLM_API_KEY}", "Content-Type": "application/json"}
    payload = {"model": LLM_MODEL, "messages": messages, "temperature": 0.2}
    resp = requests.post(url, json=payload, headers=headers, timeout=(LLM_CONNECT_TIMEOUT, LLM_TIMEOUT))
    resp.raise_for_status()
    data = resp.json()
    return data["choices"][0]["message"]["content"].strip()


def _ollama_chat(messages: List[Dict[str, str]]) -> str:
    payload = {"model": OLLAMA_CHAT_MODEL, "messages": messages, "stream": False}
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
        if intent == "Reaction":
            adverse, other = _split_reaction_rows(rows)
            lists_text.append(f"{zh_title}（{scoring_note}，优先列出不良反应）：")
            for i, r in enumerate(adverse, 1):
                en = r["text_en"]
                cn_hint = r.get("text_cn_hint")
                freq = r.get("freq")
                cnt = r.get("count", 1)
                item = f"{i}. {cn_hint}（{en}）" if cn_hint else f"{i}. {en}"
                if isinstance(freq, int) and freq > 0:
                    item += f"（freq={freq}）"
                elif cnt > 1:
                    item += f"（出现{cnt}次）"
                lists_text.append(item)

            if other:
                lists_text.append("其他报告事件（不等同于不良反应，不要解释为不良反应）：")
                for r in other:
                    en = r["text_en"]
                    cn_hint = r.get("text_cn_hint")
                    freq = r.get("freq")
                    item = f"- {cn_hint}（{en}）" if cn_hint else f"- {en}"
                    if isinstance(freq, int) and freq > 0:
                        item += f"（freq={freq}）"
                    lists_text.append(item)
        else:
            lists_text.append(f"{zh_title}（{scoring_note}）：")
            for i, r in enumerate(rows, 1):
                en = r["text_en"]
                cn_hint = r.get("text_cn_hint")
                freq = r.get("freq")
                cnt = r.get("count", 1)
                item = f"{i}. {cn_hint}（{en}）" if cn_hint else f"{i}. {en}"
                if isinstance(freq, int) and freq > 0:
                    item += f"（freq={freq}）"
                elif cnt > 1:
                    item += f"（出现{cnt}次）"
                lists_text.append(item)
    context = "\n".join(lists_text)

    prompt = f"""请用简体中文回答下面的问题，并严格基于给定要点进行归纳：
要求：
1. 只基于“原始要点”，不得编造、猜测、延伸未给出的事实；
2. 不做因果推断/用药建议/剂量推断（例如不要把“dose omission issue”解释为“剂量不足导致XX”）；
3. 将“用药错误/产品问题/疾病诊断/非不良反应”的条目单独归为“其他报告事件”，不要当作不良反应解释；
4. 合并相近概念，不机械逐条照搬；
5. 药物名可保留英文，若常见可加中文；
6. 使用分点或分段，简洁专业；
7. 如原始要点里带有 freq，请在条目后保留（表示报告频次，不代表发生率）。
8. 重要：对于原始要点中形如“中文（英文术语）”或“英文术语”的条目，请在输出中保留英文术语原样出现（可放在括号中），不要把英文术语完全翻译/改写掉；至少保留 3 个英文术语原样出现。
8. 不要原样复制“原始要点”列表；只输出回答内容。

输出格式建议：
- 先给出“不良反应（按freq）”的要点列表（3-8条，带freq）；
- 如存在“其他报告事件”，单独给一小段列出（带freq），并明确说明“不是不良反应”。

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


def _is_llm_answer_grounded(answer: str, per_intent: Dict[str, List[Dict[str, Any]]]) -> bool:
    """Best-effort grounding check.

    We only have reliable *English* surface forms from KG nodes.
    If LLM output doesn't mention any of them, it is likely hallucinating
    (or translating freely), so we fall back to deterministic local output.
    """

    text = (answer or "").strip()
    if not text:
        return False

    hay = text.lower()

    # Reaction intent is the most visible; be strict.
    strict_intents = {"Reaction"}

    for intent, rows in (per_intent or {}).items():
        if not rows:
            continue

        # Check top N for each intent.
        n = 8 if intent in strict_intents else 5
        candidates = []
        for r in rows[:n]:
            en = (r.get("text_en") or "").strip()
            if en:
                candidates.append(en.lower())

        if not candidates:
            continue

        if not any(c in hay for c in candidates):
            # Not grounded for this intent.
            return False

    return True


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
            if _is_llm_answer_grounded(llm_text, per_intent_for_llm):
                ok = True
                err = ""
            else:
                ok = False
                err = "LLM output not grounded; fallback to local enumeration"
                first_mode = next(iter(scoring_mode_by_intent.values()), "uniform_sim")
                note = "（仅语义相关排序，LLM输出不可靠回退枚举）" if first_mode == "uniform_sim" else "（综合语义与频次，LLM输出不可靠回退枚举）"
                llm_text = _local_compose_answer(per_intent_for_llm, note)
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
            "answer_version": ANSWER_LAYER_VERSION,
            "llm_model": OLLAMA_CHAT_MODEL if LLM_PROVIDER == "ollama" else LLM_MODEL,
            "llm_used": USE_LLM,
            "llm_success": ok,
            "llm_error": None if ok else err,
        },
    }
