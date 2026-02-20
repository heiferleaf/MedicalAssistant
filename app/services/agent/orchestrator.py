import time
import uuid
from typing import Any, Dict, Optional, Tuple

from app.services.agent.memory.sqlite_memory import SQLiteMemory
from app.services.agent.router import RouteDecision, route_message
from app.services.agent.tools import registry
from app.services.agent.tools.rag_tool import rag_query_tool
from app.services.agent.tools.spring_tool import spring_plan_create_tool


AGENT_VERSION = "v1_light_rules_2026-02-19"


class AgentOrchestrator:
    def __init__(self, *, memory: Optional[SQLiteMemory] = None) -> None:
        self.memory = memory or SQLiteMemory()
        # Register tools
        if not registry.has("rag.query"):
            registry.register(
                "rag.query",
                rag_query_tool,
                description="Query RAG service for medical information",
                params_schema={"question": "string", "with_trace": "boolean", "with_timing": "boolean"}
            )
        if not registry.has("spring.plan.create"):
            registry.register(
                "spring.plan.create",
                spring_plan_create_tool,
                description="Create medication reminder plan",
                params_schema={"medicineName": "string", "dosage": "string", "startDate": "string", "endDate": "string", "timePoints": "array", "remark": "string"}
            )

    def chat(
        self,
        *,
        user_id: str,
        session_id: str,
        message: str,
        with_trace: bool = False,
        with_timing: bool = False,
        client_context: Optional[Dict[str, Any]] = None,
    ) -> Dict[str, Any]:
        client_context = client_context or {}
        self.memory.append_message(session_id=session_id, user_id=user_id, role="user", content=message)

        timings: Dict[str, float] = {}
        trace: Dict[str, Any] = {
            "agent_version": AGENT_VERSION,
        }

        def _mark_timing(label: str, dt: float) -> None:
            if with_timing:
                timings[label] = dt

        t0 = time.time()
        decision: RouteDecision = route_message(message, user_id=user_id, session_id=session_id)
        _mark_timing("route", time.time() - t0)

        if with_trace:
            trace["decision"] = {
                "intent": decision.intent,
                "need_confirm": decision.need_confirm,
            }

        if decision.clarification:
            assistant_message = decision.clarification
            self.memory.append_message(session_id=session_id, user_id=user_id, role="assistant", content=assistant_message)
            result: Dict[str, Any] = {
                "success": True,
                "assistant_message": assistant_message,
                "need_confirm": False,
                "actions": [],
            }
            if with_timing:
                result["timings"] = timings
            if with_trace:
                result["trace"] = trace
            return result

        if decision.intent == "rag.query":
            t1 = time.time()
            tool_info = registry.get("rag.query")
            if tool_info:
                rag_result = tool_info["function"](
                    question=str(decision.tool_args.get("question") or ""),
                    with_trace=with_trace,
                    with_timing=with_timing,
                )
            else:
                rag_result = rag_query_tool(
                    question=str(decision.tool_args.get("question") or ""),
                    with_trace=with_trace,
                    with_timing=with_timing,
                )
            _mark_timing("tool.rag.query", time.time() - t1)

            assistant_message = (rag_result.get("answer") or "").strip()
            if not assistant_message:
                assistant_message = "我没能生成回答，请换个问法再试一次。"

            self.memory.append_message(session_id=session_id, user_id=user_id, role="assistant", content=assistant_message)

            result = {
                "success": bool(rag_result.get("success", True)),
                "assistant_message": assistant_message,
                "need_confirm": False,
                "actions": [],
                "raw": {"rag": rag_result},
            }
            if with_timing:
                result["timings"] = timings
            if with_trace:
                trace["tools_called"] = ["rag.query"]
                # keep rag trace inside raw, but also provide a quick view
                trace["rag"] = {
                    "relation_agg_version": (((rag_result.get("trace") or {}).get("aggregated") or {}).get("meta") or {}).get("version"),
                    "answer_version": (((rag_result.get("raw") or {}).get("meta") or {}).get("answer_version")),
                }
                result["trace"] = trace
            return result

        if decision.intent == "spring.plan.create.preview":
            action_id = f"act_{uuid.uuid4().hex[:16]}"
            preview = {
                "medicineName": decision.tool_args.get("medicineName"),
                "dosage": decision.tool_args.get("dosage"),
                "startDate": decision.tool_args.get("startDate"),
                "endDate": decision.tool_args.get("endDate"),
                "timePoints": decision.tool_args.get("timePoints"),
                "remark": decision.tool_args.get("remark"),
            }

            self.memory.save_pending_action(
                action_id=action_id,
                session_id=session_id,
                user_id=user_id,
                action_type="spring.plan.create",
                preview=preview,
                tool_args=dict(decision.tool_args),
                expires_in_sec=300,
            )

            assistant_message = "我将为你创建如下用药提醒/计划，请确认是否保存："
            self.memory.append_message(session_id=session_id, user_id=user_id, role="assistant", content=assistant_message)

            result = {
                "success": True,
                "assistant_message": assistant_message,
                "need_confirm": True,
                "confirm": {
                    "action_id": action_id,
                    "action_type": "spring.plan.create",
                    "preview": preview,
                    "expires_in_sec": 300,
                },
                "actions": [],
            }
            if with_timing:
                result["timings"] = timings
            if with_trace:
                trace["tools_called"] = ["spring.plan.create.preview"]
                result["trace"] = trace
            return result

        # default
        assistant_message = "我暂时无法处理这个请求。"
        self.memory.append_message(session_id=session_id, user_id=user_id, role="assistant", content=assistant_message)
        result = {
            "success": True,
            "assistant_message": assistant_message,
            "need_confirm": False,
            "actions": [],
        }
        if with_timing:
            result["timings"] = timings
        if with_trace:
            trace["decision"] = {"intent": decision.intent}
            result["trace"] = trace
        return result

    def confirm(
        self,
        *,
        user_id: str,
        session_id: str,
        action_id: str,
        confirm: bool,
        with_trace: bool = False,
        with_timing: bool = False,
    ) -> Dict[str, Any]:
        timings: Dict[str, float] = {}
        trace: Dict[str, Any] = {"agent_version": AGENT_VERSION}

        pending = self.memory.get_pending_action(action_id)
        if not pending:
            return {"success": False, "error": "action_id 不存在或已过期", "status": 404}

        if pending.get("user_id") != user_id or pending.get("session_id") != session_id:
            return {"success": False, "error": "action_id 不匹配", "status": 403}

        if pending.get("status") != "pending":
            return {"success": False, "error": f"action 状态不可确认: {pending.get('status')}", "status": 409}

        if not confirm:
            self.memory.update_pending_action_status(action_id, status="canceled")
            assistant_message = "好的，已取消本次操作。"
            self.memory.append_message(session_id=session_id, user_id=user_id, role="assistant", content=assistant_message)
            return {
                "success": True,
                "assistant_message": assistant_message,
                "need_confirm": False,
                "actions": [],
            }

        action_type = pending.get("action_type")
        tool_args = pending.get("tool_args") or {}

        if action_type == "spring.plan.create":
            t0 = time.time()
            tool_info = registry.get("spring.plan.create")
            if tool_info:
                tool_result = tool_info["function"](tool_args)
            else:
                tool_result = spring_plan_create_tool(tool_args)
            if with_timing:
                timings["tool.spring.plan.create"] = time.time() - t0

            if tool_result.get("success"):
                self.memory.update_pending_action_status(action_id, status="done", result=tool_result)
                assistant_message = "好的，已为你创建用药计划。"
                actions = [
                    {
                        "type": "spring.plan.create",
                        "status": "ok",
                        "data": tool_result.get("data"),
                    }
                ]
            else:
                self.memory.update_pending_action_status(action_id, status="failed", result=tool_result)
                assistant_message = f"创建失败：{tool_result.get('error') or 'unknown'}"
                actions = [
                    {
                        "type": "spring.plan.create",
                        "status": "error",
                        "data": tool_result,
                    }
                ]

            self.memory.append_message(session_id=session_id, user_id=user_id, role="assistant", content=assistant_message)

            result: Dict[str, Any] = {
                "success": bool(tool_result.get("success")),
                "assistant_message": assistant_message,
                "need_confirm": False,
                "actions": actions,
            }
            if with_timing:
                result["timings"] = timings
            if with_trace:
                trace["tools_called"] = ["spring.plan.create"]
                result["trace"] = trace
            return result

        return {"success": False, "error": f"不支持的 action_type: {action_type}", "status": 400}
