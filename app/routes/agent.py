from typing import Any, Dict

import os
import socket
from datetime import datetime, timezone

from flask import Blueprint, jsonify, request

from app.services.agent.orchestrator import AgentOrchestrator

agent_bp = Blueprint("agent", __name__)


@agent_bp.get("/health")
def agent_health() -> Any:
    now_utc = datetime.now(timezone.utc)
    return jsonify(
        {
            "status": "ok",
            "module": "agent",
            "hostname": socket.gethostname(),
            "pid": os.getpid(),
            "server_time_utc": now_utc.isoformat(),
            "request_host": request.host,
        }
    )


@agent_bp.post("/chat")
def agent_chat() -> Any:
    payload: Dict[str, Any] = request.get_json(silent=True) or {}
    user_id = str(payload.get("user_id") or "").strip()
    session_id = str(payload.get("session_id") or "").strip()
    message = str(payload.get("message") or "").strip()
    if not user_id or not session_id:
        return jsonify({"success": False, "error": "user_id 和 session_id 不能为空"}), 400
    if not message:
        return jsonify({"success": False, "error": "message 不能为空"}), 400

    orchestrator = AgentOrchestrator()
    result = orchestrator.chat(
        user_id=user_id,
        session_id=session_id,
        message=message,
        with_trace=bool(payload.get("with_trace")),
        with_timing=bool(payload.get("with_timing")),
        client_context=payload.get("client_context") if isinstance(payload.get("client_context"), dict) else None,
    )

    status = 200 if result.get("success") else result.get("status", 500)
    return jsonify(result), status


@agent_bp.post("/confirm")
def agent_confirm() -> Any:
    payload: Dict[str, Any] = request.get_json(silent=True) or {}
    user_id = str(payload.get("user_id") or "").strip()
    session_id = str(payload.get("session_id") or "").strip()
    action_id = str(payload.get("action_id") or "").strip()
    confirm_val = payload.get("confirm")

    if not user_id or not session_id:
        return jsonify({"success": False, "error": "user_id 和 session_id 不能为空"}), 400
    if not action_id:
        return jsonify({"success": False, "error": "action_id 不能为空"}), 400
    if confirm_val is None:
        return jsonify({"success": False, "error": "confirm 不能为空（true/false）"}), 400

    orchestrator = AgentOrchestrator()
    result = orchestrator.confirm(
        user_id=user_id,
        session_id=session_id,
        action_id=action_id,
        confirm=bool(confirm_val),
        with_trace=bool(payload.get("with_trace")),
        with_timing=bool(payload.get("with_timing")),
    )

    status = 200 if result.get("success") else result.get("status", 500)
    return jsonify(result), status
