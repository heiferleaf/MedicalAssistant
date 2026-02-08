from typing import Any, Dict

from flask import Blueprint, jsonify, request

from app.services.rag_service import RagService

rag_bp = Blueprint("rag", __name__)


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
