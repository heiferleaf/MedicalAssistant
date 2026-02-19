import os
from typing import List

import numpy as np

EMBEDDING_MODEL_TYPE = os.getenv("EMBEDDING_MODEL_TYPE", "biencoder").lower()
BIENCODER_MODEL_PATH = os.getenv("BIENCODER_MODEL_PATH", "")

# OpenAI-compatible embedding API (can be OpenAI, or any provider that exposes /v1/embeddings).
EMBEDDING_API_BASE = os.getenv("EMBEDDING_API_BASE", "").strip()
EMBEDDING_API_KEY = os.getenv("EMBEDDING_API_KEY", "").strip()
EMBEDDING_MODEL = os.getenv("EMBEDDING_MODEL", "text-embedding-3-small").strip()

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
# 兼容：历史上 embedding 与 chat 共用 OLLAMA_MODEL。
# 现在建议拆分为 OLLAMA_EMBED_MODEL / OLLAMA_CHAT_MODEL，避免误用。
OLLAMA_EMBED_MODEL = os.getenv("OLLAMA_EMBED_MODEL") or os.getenv("OLLAMA_MODEL", "nomic-embed-text")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))

_biencoder_model = None


def _l2_normalize(vec: List[float]) -> List[float]:
    arr = np.asarray(vec, dtype=np.float32)
    n = float(np.linalg.norm(arr))
    if n == 0.0:
        return arr.astype(np.float32).tolist()
    return (arr / n).astype(np.float32).tolist()


def _normalize_openai_base(base_url: str) -> str:
    """Normalize OpenAI-compatible base URL.

    We accept both:
    - https://api.xxx.com
    - https://api.xxx.com/v1

    and always return without the trailing /v1 to avoid double-joining.
    """

    b = (base_url or "").strip().rstrip("/")
    if b.endswith("/v1"):
        b = b[: -len("/v1")]
    return b


def _get_biencoder_model():
    global _biencoder_model
    if _biencoder_model is None:
        if not BIENCODER_MODEL_PATH:
            raise RuntimeError("BIENCODER_MODEL_PATH 未配置")
        from sentence_transformers import SentenceTransformer

        _biencoder_model = SentenceTransformer(BIENCODER_MODEL_PATH)
    return _biencoder_model


def embed_ollama(text: str) -> List[float]:
    import requests

    url = f"{OLLAMA_BASE_URL}/api/embeddings"
    payload = {"model": OLLAMA_EMBED_MODEL, "prompt": text or ""}
    r = requests.post(url, json=payload, timeout=OLLAMA_TIMEOUT)
    r.raise_for_status()
    data = r.json()
    vec = data.get("embedding")
    if not isinstance(vec, list):
        raise RuntimeError(f"Ollama 返回格式异常: {data}")
    return _l2_normalize(vec)


def embed_openai(text: str) -> List[float]:
    import requests

    api_key = (
        EMBEDDING_API_KEY
        or os.getenv("OPENAI_API_KEY", "")
        or os.getenv("LLM_API_KEY", "")
        or os.getenv("DASHSCOPE_API_KEY", "")
    ).strip()
    if not api_key:
        raise RuntimeError("EMBEDDING_API_KEY 未配置（也未检测到 OPENAI_API_KEY/LLM_API_KEY）")

    base = EMBEDDING_API_BASE.strip() or os.getenv("OPENAI_BASE_URL", "").strip() or os.getenv("LLM_API_BASE", "").strip()
    base = _normalize_openai_base(base)
    if not base:
        raise RuntimeError("EMBEDDING_API_BASE 未配置（也未检测到 OPENAI_BASE_URL/LLM_API_BASE）")

    url = f"{base}/v1/embeddings"
    headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
    payload = {"model": EMBEDDING_MODEL, "input": text or ""}
    resp = requests.post(url, json=payload, headers=headers, timeout=OLLAMA_TIMEOUT)
    resp.raise_for_status()
    data = resp.json()
    vec = (((data or {}).get("data") or [{}])[0] or {}).get("embedding")
    if not isinstance(vec, list):
        raise RuntimeError(f"Embedding API 返回格式异常: {data}")
    return _l2_normalize(vec)


def embed_biencoder(text: str) -> List[float]:
    model = _get_biencoder_model()
    vec = model.encode(text, normalize_embeddings=True)
    return vec.tolist() if hasattr(vec, "tolist") else list(vec)


def embed_text(text: str) -> List[float]:
    if not text or not text.strip():
        raise ValueError("输入文本不能为空")

    text = text.strip()
    if EMBEDDING_MODEL_TYPE == "biencoder":
        return embed_biencoder(text)
    if EMBEDDING_MODEL_TYPE == "openai":
        return embed_openai(text)
    return embed_ollama(text)


def get_current_model_name() -> str:
    if EMBEDDING_MODEL_TYPE == "biencoder":
        return f"biencoder({BIENCODER_MODEL_PATH})"
    if EMBEDDING_MODEL_TYPE == "openai":
        return f"openai_compat({EMBEDDING_MODEL})"
    return f"ollama({OLLAMA_EMBED_MODEL})"
