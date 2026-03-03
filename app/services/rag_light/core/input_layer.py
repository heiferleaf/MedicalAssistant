import json
import os
import re
from typing import Any, Dict, List

import requests
from openai import OpenAI

# 控制是否使用向量嵌入
USE_EMBEDDING = os.getenv("USE_EMBEDDING", "true").lower() == "true"

from app.services.rag_light.core.embedding_utils import embed_text, get_current_model_name

OPENAI_API_KEY = (
    os.getenv("OPENAI_API_KEY", "")
    or os.getenv("LLM_API_KEY", "")
    or os.getenv("DASHSCOPE_API_KEY", "")
).strip()
OPENAI_BASE_URL = (os.getenv("OPENAI_BASE_URL", "") or os.getenv("LLM_API_BASE", "") or "https://api.openai.com").strip()
OPENAI_MODEL = (os.getenv("OPENAI_MODEL", "") or os.getenv("LLM_MODEL", "") or "gpt-3.5-turbo").strip()

# 输入解析层可选择：openai / ollama。
INPUT_PROVIDER = os.getenv("INPUT_PROVIDER", "openai").strip().lower()

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_CHAT_MODEL = os.getenv("OLLAMA_CHAT_MODEL") or os.getenv("OLLAMA_MODEL", "qwen2.5:3b-instruct")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))

SYSTEM_EXTRACT = (
    "你是药物安全知识图谱的解析器。图谱只有四类节点：Drug、Reaction、Indication、Outcome。\n"
    "请根据用户问题严格输出 JSON：\n"
    "{\n"
    "  \"seeds\": [\n"
    "    {\"type\": \"Drug|Reaction|Indication|Outcome\", \"text\": \"...\"}\n"
    "  ],\n"
    "  \"intents\": [\"Drug|Reaction|Indication|Outcome\"],\n"
    "  \"topk\": 10\n"
    "}\n"
    "说明：\n"
    "- seeds 可以包含多条，允许不同类型同时出现，只要问题中提到了即可。\n"
    "- intents 表示用户想查询/返回的节点类型；可包含多个。\n"
    "- topk 默认为 10，除非用户要求其它数量。\n"
    "- 只输出一个 JSON 对象，不要额外文字。"
)

SYSTEM_TRANSLATE = (
    "你是医学术语翻译助手。把输入的药物/反应/适应症/结果名称翻译成英文常用名，"
    "仅输出译文本身，不要解释。若已是英文则原样返回。"
)


# 常用药物名的快速映射（减少一次 LLM 翻译耗时，并提升与 FAERS/图谱用词的一致性）。
# 说明：FAERS 场景里更常见的是 ACETAMINOPHEN（美式），而不是 PARACETAMOL（英式）。
_DRUG_TRANSLATION_OVERRIDES = {
    "阿司匹林": "Aspirin",
    "布洛芬": "Ibuprofen",
    "对乙酰氨基酚": "Acetaminophen",
    "扑热息痛": "Acetaminophen",
    "泰诺": "Acetaminophen",
}


_DRUG_SYNONYM_NORMALIZATION = {
    "paracetamol": "Acetaminophen",
    "acetaminophen": "Acetaminophen",
}


def _client() -> OpenAI | None:
    if not OPENAI_API_KEY:
        return None
    base = (OPENAI_BASE_URL or "").strip().rstrip("/")
    # OpenAI SDK expects base_url like https://host/v1
    if not base.endswith("/v1"):
        base = base + "/v1"
    return OpenAI(api_key=OPENAI_API_KEY, base_url=base)


def _ollama_chat(system: str, user: str) -> str:
    payload = {
        "model": OLLAMA_CHAT_MODEL,
        "messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ],
        "stream": False,
    }
    resp = requests.post(
        f"{OLLAMA_BASE_URL}/api/chat",
        json=payload,
        timeout=OLLAMA_TIMEOUT,
    )
    resp.raise_for_status()
    data = resp.json()
    content = (data.get("message") or {}).get("content", "")
    return (content or "").strip()


def _extract_json(text: str) -> Dict[str, Any]:
    try:
        match = re.search(r"\{.*\}", text, re.S)
        return json.loads(match.group(0)) if match else {}
    except Exception:
        return {}


def _translate_to_en(text: str) -> str:
    t = (text or "").strip()
    if not t:
        return t

    # Fast-path overrides for common drug names.
    if t in _DRUG_TRANSLATION_OVERRIDES:
        return _DRUG_TRANSLATION_OVERRIDES[t]

    if all(ord(ch) < 128 for ch in t):
        norm = _DRUG_SYNONYM_NORMALIZATION.get(t.strip().lower())
        return norm or t
    if INPUT_PROVIDER == "ollama":
        try:
            translated = _ollama_chat(SYSTEM_TRANSLATE, t)
            translated = (translated or t).strip()
            norm = _DRUG_SYNONYM_NORMALIZATION.get(translated.lower())
            return norm or translated
        except Exception:
            return t

    client = _client()
    if client is None:
        return t
    try:
        resp = client.chat.completions.create(
            model=OPENAI_MODEL,
            messages=[
                {"role": "system", "content": SYSTEM_TRANSLATE},
                {"role": "user", "content": t},
            ],
            temperature=0.0,
        )
        translated = resp.choices[0].message.content.strip()
        translated = translated or t
        norm = _DRUG_SYNONYM_NORMALIZATION.get(translated.strip().lower())
        return norm or translated
    except Exception:
        return t


def _rule_based_parse(question: str) -> Dict[str, Any]:
    """基于规则的输入解析，替代LLM调用"""
    seeds = []
    intents = ["Drug", "Reaction", "Indication", "Outcome"]
    
    # 简单的关键词匹配
    question_lower = question.lower()
    
    # 检查是否包含药物相关关键词
    drug_keywords = ["药物", "药", "药品", "medication", "drug", "medicine"]
    if any(keyword in question_lower for keyword in drug_keywords):
        seeds.append({"type": "Drug", "text": "药物", "normalized": "Drug"})
    
    # 检查是否包含不良反应相关关键词
    reaction_keywords = ["不良反应", "副作用", "副作用", "adverse", "reaction", "side effect"]
    if any(keyword in question_lower for keyword in reaction_keywords):
        seeds.append({"type": "Reaction", "text": "不良反应", "normalized": "Reaction"})
    
    # 检查是否包含适应症相关关键词
    indication_keywords = ["适应症", "用途", "适应症", "indication", "use", "purpose"]
    if any(keyword in question_lower for keyword in indication_keywords):
        seeds.append({"type": "Indication", "text": "适应症", "normalized": "Indication"})
    
    # 检查是否包含结局相关关键词
    outcome_keywords = ["结局", "结果", "outcome", "result"]
    if any(keyword in question_lower for keyword in outcome_keywords):
        seeds.append({"type": "Outcome", "text": "结局", "normalized": "Outcome"})
    
    return {
        "seeds": seeds,
        "intents": intents,
        "topk": 10
    }

def parse_input(question: str) -> Dict[str, Any]:
    raw = "{}"
    
    # 首先尝试使用规则解析，提高性能
    rule_based_data = _rule_based_parse(question)
    
    # 如果规则解析成功（识别到种子），直接使用规则解析结果
    if rule_based_data.get('seeds'):
        data = rule_based_data
    else:
        # 规则解析失败时，使用LLM进行解析
        if INPUT_PROVIDER == "ollama":
            try:
                raw = _ollama_chat(SYSTEM_EXTRACT, question)
            except Exception:
                raw = "{}"
        else:
            client = _client()
            if client is not None:
                try:
                    resp = client.chat.completions.create(
                        model=OPENAI_MODEL,
                        messages=[
                            {"role": "system", "content": SYSTEM_EXTRACT},
                            {"role": "user", "content": question},
                        ],
                        temperature=0.0,
                    )
                    raw = resp.choices[0].message.content.strip()
                except Exception:
                    raw = "{}"
        data = _extract_json(raw)

    seeds_raw = data.get("seeds") or []
    intents_raw = data.get("intents") or []
    topk = int(data.get("topk") or 10)

    valid_types = {"Drug", "Reaction", "Indication", "Outcome"}
    seeds: List[Dict[str, str]] = []
    for item in seeds_raw:
        t = item.get("type")
        txt = item.get("text", "")
        if t in valid_types and isinstance(txt, str) and txt.strip():
            seeds.append(
                {
                    "type": t,
                    "text": txt.strip(),
                    "normalized": _translate_to_en(txt.strip()),
                }
            )

    intents = [t for t in intents_raw if t in valid_types]
    if not intents:
        intents = ["Drug", "Reaction", "Indication", "Outcome"]

    try:
        if USE_EMBEDDING:
            query_vector = embed_text(question)
        else:
            query_vector = None
    except Exception:
        query_vector = None

    result: Dict[str, Any] = {
        "question": question,
        "seeds": seeds,
        "intents": intents,
        "topk": topk,
        "query_vector": query_vector,
        "meta": {
            "raw_llm_output": raw,
            "input_provider": INPUT_PROVIDER,
            "parsed_ok": bool(seeds or intents),
            "embedding_model": get_current_model_name(),
            "query_vector_dim": len(query_vector) if query_vector else None,
        },
    }
    return result
