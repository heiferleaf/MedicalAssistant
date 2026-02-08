import os
from typing import Dict, List

import requests

LLM_API_BASE = os.getenv("LLM_API_BASE", "").strip()
LLM_API_KEY = os.getenv("LLM_API_KEY", "").strip()
LLM_MODEL = os.getenv("LLM_MODEL", "gpt-3.5-turbo").strip()
LLM_TIMEOUT = float(os.getenv("LLM_TIMEOUT", "60"))


def _build_headers() -> Dict[str, str]:
    headers = {"Content-Type": "application/json"}
    if LLM_API_KEY:
        headers["Authorization"] = f"Bearer {LLM_API_KEY}"
    return headers


def chat(messages: List[Dict[str, str]]) -> str:
    if not LLM_API_BASE:
        raise RuntimeError("LLM_API_BASE 未配置")

    url = LLM_API_BASE.rstrip("/") + "/v1/chat/completions"
    payload = {
        "model": LLM_MODEL,
        "messages": messages,
        "temperature": 0.2,
    }

    resp = requests.post(url, json=payload, headers=_build_headers(), timeout=LLM_TIMEOUT)
    resp.raise_for_status()
    data = resp.json()
    return data["choices"][0]["message"]["content"].strip()
