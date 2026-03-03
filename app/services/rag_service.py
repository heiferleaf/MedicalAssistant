import os
import sys
from typing import Any, Dict

import requests

from app.services.rag_light import rag_query

RAG_PROXY_URL = os.getenv("RAG_PROXY_URL", "").strip()
RAG_PROJECT_PATH = os.getenv("RAG_PROJECT_PATH", "").strip()
TIMEOUT_SECONDS = float(os.getenv("RAG_TIMEOUT", "120"))
RAG_LOCAL_MODE = os.getenv("RAG_LOCAL_MODE", "true").lower() == "true"

rag_query_func = None
if RAG_PROJECT_PATH:
    if RAG_PROJECT_PATH not in sys.path:
        sys.path.insert(0, RAG_PROJECT_PATH)
try:
    from rag import rag_query as _rag_query  # type: ignore

    rag_query_func = _rag_query
except Exception:
    rag_query_func = None


class RagService:
    def query(self, *, question: str, with_trace: bool, with_timing: bool) -> Dict[str, Any]:
        if RAG_LOCAL_MODE:
            return rag_query(
                question,
                with_trace=with_trace,
                with_timing=with_timing,
                suppress_internal_logs=True,
            )

        # 优先本地调用 rag_query（本地测试）
        if rag_query_func is not None:
            try:
                result = rag_query_func(
                    question,
                    with_trace=with_trace,
                    with_timing=with_timing,
                    suppress_internal_logs=True,
                )
                return result
            except Exception as exc:  # noqa: BLE001
                return {"success": False, "error": str(exc), "status": 500}

        if not RAG_PROXY_URL:
            return {
                "success": False,
                "error": "RAG_PROXY_URL 未配置，且本地 rag_query 不可用",
                "status": 503,
            }

        body = {
            "question": question,
            "with_trace": with_trace,
            "with_timing": with_timing,
            "suppress_internal_logs": True,
        }

        try:
            resp = requests.post(RAG_PROXY_URL, json=body, timeout=TIMEOUT_SECONDS)
            data = resp.json()
            data["status"] = resp.status_code
            return data
        except Exception as exc:  # noqa: BLE001
            return {"success": False, "error": str(exc), "status": 502}
