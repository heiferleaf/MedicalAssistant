import os
from typing import List

import numpy as np

EMBEDDING_MODEL_TYPE = os.getenv("EMBEDDING_MODEL_TYPE", "biencoder").lower()
BIENCODER_MODEL_PATH = os.getenv("BIENCODER_MODEL_PATH", "")

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "nomic-embed-text")
OLLAMA_TIMEOUT = float(os.getenv("OLLAMA_TIMEOUT", "120"))

_biencoder_model = None


def _l2_normalize(vec: List[float]) -> List[float]:
    arr = np.asarray(vec, dtype=np.float32)
    n = float(np.linalg.norm(arr))
    if n == 0.0:
        return arr.astype(np.float32).tolist()
    return (arr / n).astype(np.float32).tolist()


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
    payload = {"model": OLLAMA_MODEL, "prompt": text or ""}
    r = requests.post(url, json=payload, timeout=OLLAMA_TIMEOUT)
    r.raise_for_status()
    data = r.json()
    vec = data.get("embedding")
    if not isinstance(vec, list):
        raise RuntimeError(f"Ollama 返回格式异常: {data}")
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
    return embed_ollama(text)


def get_current_model_name() -> str:
    if EMBEDDING_MODEL_TYPE == "biencoder":
        return f"biencoder({BIENCODER_MODEL_PATH})"
    return f"ollama({OLLAMA_MODEL})"
