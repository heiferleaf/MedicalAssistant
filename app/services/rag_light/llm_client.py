import os
from typing import Dict, List

import requests

LLM_PROVIDER = os.getenv("LLM_PROVIDER", "openai").strip().lower()

LLM_API_BASE = os.getenv("LLM_API_BASE", "").strip()
LLM_API_KEY = (
    os.getenv("LLM_API_KEY", "")
    or os.getenv("OPENAI_API_KEY", "")
    or os.getenv("DASHSCOPE_API_KEY", "")
).strip()
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-3.5-turbo").strip()
LLM_CONNECT_TIMEOUT = float(os.getenv("LLM_CONNECT_TIMEOUT", "10"))
LLM_TIMEOUT = float(os.getenv("LLM_TIMEOUT", "60"))

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
OLLAMA_CHAT_MODEL = os.getenv("OLLAMA_CHAT_MODEL") or os.getenv("OLLAMA_MODEL", "qwen2.5:7b-instruct")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))


def _build_headers() -> Dict[str, str]:
    headers = {"Content-Type": "application/json"}
    if LLM_API_KEY:
        headers["Authorization"] = f"Bearer {LLM_API_KEY}"
    return headers


def _normalize_openai_base(base_url: str) -> str:
    b = (base_url or "").strip().rstrip("/")
    if b.endswith("/v1"):
        b = b[: -len("/v1")]
    return b


def _ollama_chat(messages: List[Dict[str, str]]) -> str:
    payload = {"model": OLLAMA_CHAT_MODEL, "messages": messages, "stream": False}
    resp = requests.post(f"{OLLAMA_BASE_URL}/api/chat", json=payload, timeout=OLLAMA_TIMEOUT)
    resp.raise_for_status()
    data = resp.json()
    content = (data.get("message") or {}).get("content", "")
    return (content or "").strip()


def chat(messages: List[Dict[str, str]]) -> str:
    if LLM_PROVIDER == "ollama":
        return _ollama_chat(messages)

    if not LLM_API_BASE:
        raise RuntimeError("LLM_API_BASE 未配置")

    base = _normalize_openai_base(LLM_API_BASE)
    url = base + "/v1/chat/completions"
    payload = {"model": LLM_MODEL, "messages": messages, "temperature": 0.2}
    resp = requests.post(url, json=payload, headers=_build_headers(), timeout=(LLM_CONNECT_TIMEOUT, LLM_TIMEOUT))
    resp.raise_for_status()
    data = resp.json()
    return data["choices"][0]["message"]["content"].strip()
