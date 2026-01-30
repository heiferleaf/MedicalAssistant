import os
import sys
from typing import Any, Dict

import requests
from flask import Flask, jsonify, request

app = Flask(__name__)

RAG_PROXY_URL = os.getenv("RAG_PROXY_URL", "").strip()
RAG_PROJECT_PATH = os.getenv("RAG_PROJECT_PATH", "").strip()
TIMEOUT_SECONDS = float(os.getenv("RAG_TIMEOUT", "120"))

# 尝试本地导入 rag_query（用于本地直连测试）
rag_query = None
if RAG_PROJECT_PATH:
    if RAG_PROJECT_PATH not in sys.path:
        sys.path.insert(0, RAG_PROJECT_PATH)
try:
    from rag import rag_query as _rag_query  # type: ignore

    rag_query = _rag_query
except Exception:
    rag_query = None


@app.get("/health")
def health() -> Any:
    return jsonify({"status": "ok"})


@app.post("/rag/query")
def rag_query() -> Any:
    payload: Dict[str, Any] = request.get_json(silent=True) or {}
    question = (payload.get("question") or "").strip()
    if not question:
        return jsonify({"success": False, "error": "question 不能为空"}), 400

    # 优先本地调用 rag_query（本地测试）
    if rag_query is not None:
        try:
            result = rag_query(
                question,
                with_trace=bool(payload.get("with_trace")),
                with_timing=bool(payload.get("with_timing")),
                suppress_internal_logs=True,
            )
            return jsonify(result), 200
        except Exception as exc:  # noqa: BLE001
            return jsonify({"success": False, "error": str(exc)}), 500

    if not RAG_PROXY_URL:
        return (
            jsonify({
                "success": False,
                "error": "RAG_PROXY_URL 未配置，且本地 rag_query 不可用"
            }),
            503,
        )

    body = {
        "question": question,
        "with_trace": bool(payload.get("with_trace")),
        "with_timing": bool(payload.get("with_timing")),
        "suppress_internal_logs": True,
    }

    try:
        resp = requests.post(RAG_PROXY_URL, json=body, timeout=TIMEOUT_SECONDS)
        return jsonify(resp.json()), resp.status_code
    except Exception as exc:  # noqa: BLE001
        return jsonify({"success": False, "error": str(exc)}), 502


if __name__ == "__main__":
    host = os.getenv("FLASK_HOST", "0.0.0.0")
    port = int(os.getenv("FLASK_PORT", "8001"))
    debug = os.getenv("FLASK_DEBUG", "false").lower() == "true"
    app.run(host=host, port=port, debug=debug)
