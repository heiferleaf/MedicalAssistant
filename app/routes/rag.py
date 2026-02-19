import os
import time
from typing import Any, Dict

from flask import Blueprint, jsonify, request

from app.services.rag_service import RagService
from app.services.rag_light.core.relation_aggregate_layer import RELATION_AGG_VERSION


rag_bp = Blueprint("rag", __name__)


@rag_bp.get("/health")
def rag_health() -> Any:
    checks: Dict[str, Any] = {}

    embedding_model_type = os.getenv("EMBEDDING_MODEL_TYPE", "biencoder").strip().lower()
    llm_provider = os.getenv("LLM_PROVIDER", "openai").strip().lower()
    input_provider = os.getenv("INPUT_PROVIDER", "openai").strip().lower()

    # 1) Ollama (only check if current runtime config needs it)
    needs_ollama = (embedding_model_type == "ollama") or (llm_provider == "ollama") or (input_provider == "ollama")
    ollama_base = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
    if needs_ollama:
        try:
            import requests

            t0 = time.time()
            resp = requests.get(f"{ollama_base}/api/version", timeout=3)
            latency_ms = int((time.time() - t0) * 1000)
            checks["ollama"] = {
                "ok": resp.ok,
                "status": resp.status_code,
                "latency_ms": latency_ms,
                "base_url": ollama_base,
                "body": resp.json() if resp.ok else resp.text[:2000],
            }
        except Exception as exc:  # noqa: BLE001
            checks["ollama"] = {"ok": False, "error": repr(exc), "base_url": ollama_base}
    else:
        checks["ollama"] = {"skipped": True, "needs_ollama": False, "base_url": ollama_base}

    # 2) Neo4j
    neo4j_uri = os.getenv("NEO4J_URI", "bolt://127.0.0.1:7687")
    neo4j_user = os.getenv("NEO4J_USER", "neo4j")
    neo4j_password = os.getenv("NEO4J_PASSWORD", "")
    try:
        from py2neo import Graph

        t0 = time.time()
        g = Graph(neo4j_uri, auth=(neo4j_user, neo4j_password))
        ok_val = g.run("RETURN 1 AS ok").evaluate()
        latency_ms = int((time.time() - t0) * 1000)
        checks["neo4j"] = {
            "ok": ok_val == 1,
            "latency_ms": latency_ms,
            "uri": neo4j_uri,
            "user": neo4j_user,
        }
    except Exception as exc:  # noqa: BLE001
        checks["neo4j"] = {
            "ok": False,
            "error": repr(exc),
            "uri": neo4j_uri,
            "user": neo4j_user,
        }

    # 3) Runtime meta
    checks["rag"] = {
        "rag_local_mode": os.getenv("RAG_LOCAL_MODE", "true").lower() == "true",
        "embedding_model_type": embedding_model_type,
        "llm_provider": llm_provider,
        "input_provider": input_provider,
        "needs_ollama": needs_ollama,
        "relation_agg_version": RELATION_AGG_VERSION,
    }

    neo4j_ok = bool(checks.get("neo4j", {}).get("ok"))
    ollama_ok = (not needs_ollama) or bool(checks.get("ollama", {}).get("ok"))

    # Keep HTTP semantics: only fail hard if Neo4j is unavailable.
    # But reflect partial outages in the returned status string.
    status = "ok" if (neo4j_ok and ollama_ok) else "degraded"
    http_status = 200 if neo4j_ok else 503
    return jsonify({"status": status, "checks": checks}), http_status


@rag_bp.post("/query")
def rag_query() -> Any:
    payload: Dict[str, Any] = request.get_json(silent=True) or {}
    question = (payload.get("question") or "").strip()
    if not question:
        return jsonify({"success": False, "error": "question 不能为空"}), 400

    service = RagService()
    result = service.query(
        question=question,
        with_trace=bool(payload.get("with_trace")),
        with_timing=bool(payload.get("with_timing")),
    )

    status = 200 if result.get("success") else result.get("status", 500)
    return jsonify(result), status
