from typing import Any

from flask import Blueprint, jsonify

ocr_bp = Blueprint("ocr", __name__)


@ocr_bp.get("/health")
def ocr_health() -> Any:
    return jsonify({"status": "ok", "module": "ocr"})
