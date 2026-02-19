import os
from typing import Any, Dict

import requests


SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://127.0.0.1:8080").rstrip("/")
SPRING_API_PREFIX = os.getenv("SPRING_API_PREFIX", "/api").strip()  # '/api'
SPRING_TIMEOUT = float(os.getenv("SPRING_TIMEOUT", "5"))
SPRING_SERVICE_TOKEN = os.getenv("SPRING_SERVICE_TOKEN", "").strip()


def spring_plan_create_tool(tool_args: Dict[str, Any]) -> Dict[str, Any]:
    """Call Spring Boot to create a medication plan.

    Backend: POST /api/plan?userId=...
    Body (PlanCreateDTO): medicineName, dosage?, startDate, endDate?, timePoints, remark?
    """

    user_id = tool_args.get("user_id")
    if not user_id:
        return {"success": False, "error": "missing user_id"}

    # Build URL
    prefix = SPRING_API_PREFIX
    if not prefix.startswith("/"):
        prefix = "/" + prefix
    url = f"{SPRING_BASE_URL}{prefix}/plan"

    params = {"userId": user_id}
    body = {
        "medicineName": tool_args.get("medicineName"),
        "dosage": tool_args.get("dosage"),
        "startDate": tool_args.get("startDate"),
        "endDate": tool_args.get("endDate"),
        "timePoints": tool_args.get("timePoints") or [],
        "remark": tool_args.get("remark"),
    }

    headers = {"Content-Type": "application/json"}
    if SPRING_SERVICE_TOKEN:
        headers["X-Service-Token"] = SPRING_SERVICE_TOKEN

    try:
        resp = requests.post(url, params=params, json=body, headers=headers, timeout=SPRING_TIMEOUT)
        # Spring Result wrapper: {code,msg,data}
        data = resp.json() if resp.content else {}
        ok = resp.ok
        if not ok:
            return {"success": False, "error": f"spring_http_{resp.status_code}", "data": data}
        return {"success": True, "data": data}
    except Exception as exc:  # noqa: BLE001
        return {"success": False, "error": repr(exc)}
