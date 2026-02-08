from typing import Any

from flask import Blueprint, jsonify

agent_bp = Blueprint("agent", __name__)


@agent_bp.get("/health")
def agent_health() -> Any:
    return jsonify({"status": "ok", "module": "agent"})
