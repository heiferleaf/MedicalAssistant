import re
from dataclasses import dataclass
from datetime import date
from typing import Any, Dict, Optional, Tuple


_REMINDER_KEYWORDS = ("提醒", "闹钟", "定时", "每天", "每日", "设置", "新增", "创建", "添加")


@dataclass
class RouteDecision:
    intent: str  # 'rag.query' | 'spring.plan.create.preview' | 'other'
    tool_args: Dict[str, Any]
    need_confirm: bool = False
    clarification: Optional[str] = None


def _looks_like_reminder(text: str) -> bool:
    t = (text or "").strip()
    if not t:
        return False
    return any(k in t for k in _REMINDER_KEYWORDS) and ("提醒" in t or "闹钟" in t)


def _parse_time_of_day(text: str) -> Optional[str]:
    """Parse a time from Chinese text.

    Returns "HH:MM" or None.
    """

    t = (text or "")
    # Examples: 20:00, 8:00, 8点, 8点30, 晚上8点, 下午3点
    m = re.search(r"(?P<h>\d{1,2})(?:\s*[:：]\s*(?P<m>\d{1,2}))?\s*(?:点|时)?\s*(?P<m2>\d{1,2})?\s*(?:分)?", t)
    if not m:
        return None

    hour = int(m.group("h"))
    minute = None
    if m.group("m") is not None:
        minute = int(m.group("m"))
    elif m.group("m2") is not None:
        minute = int(m.group("m2"))
    else:
        minute = 0

    # Normalize by day period hints
    lower = t.lower()
    if any(k in t for k in ("下午", "晚上", "夜里", "夜间")):
        if 1 <= hour <= 11:
            hour += 12
    if any(k in t for k in ("中午",)):
        if hour == 0:
            hour = 12
        elif 1 <= hour <= 10:
            hour += 12

    if hour < 0 or hour > 23 or minute < 0 or minute > 59:
        return None

    return f"{hour:02d}:{minute:02d}"


def _parse_medicine_name(text: str) -> Optional[str]:
    """Best-effort extract medicine name from reminder phrasing."""

    t = (text or "").strip()
    if not t:
        return None

    def _clean(med: str) -> str:
        s = (med or "").strip()
        # Strip common command prefixes accidentally captured
        s = re.sub(r"^(?:请|帮我|麻烦|我要|我想|给我)?", "", s).strip()
        s = re.sub(r"^(?:设置|创建|新增|添加|设定|定|定个|建|新建)", "", s).strip()
        s = re.sub(r"^(?:一条|一个|个|条)", "", s).strip()
        s = re.sub(r"^(?:用药)?(?:提醒|闹钟)", "", s).strip()
        return s

    # Pattern A: explicit verb
    m = re.search(
        r"(?:请|帮我|麻烦|我要|我想|给我)?\s*(?:设置|创建|新增|添加|设定|定|定个|新建)\s*(?:一条|一个|个|条)?\s*(?P<med>[\u4e00-\u9fffA-Za-z0-9\-\s]{1,32}?)\s*(?:每天|每日|每晚|每早|每周|提醒|闹钟)",
        t,
    )
    if m:
        med = _clean(m.group("med") or "")
        return med or None

    # Pattern B: medicine name directly followed by schedule/clock keyword
    m = re.search(
        r"(?P<med>[\u4e00-\u9fffA-Za-z0-9\-\s]{1,32}?)\s*(?:每天|每日|每晚|每早|每周)\s*.*?(?:提醒|闹钟)",
        t,
    )
    if m:
        med = _clean(m.group("med") or "")
        return med or None

    # Fallback: try capture after '提醒'
    m2 = re.search(r"提醒\s*(?P<med>[\u4e00-\u9fffA-Za-z0-9\-\s]{1,32})", t)
    if m2:
        med = _clean(m2.group("med") or "")
        return med or None

    return None


def route_message(message: str, *, user_id: str, session_id: str) -> RouteDecision:
    """Rule-first lightweight routing.

    v1 policy:
    - Reminder/clock -> create Spring plan (preview only, needs confirm)
    - Otherwise -> RAG query
    """

    text = (message or "").strip()
    if not text:
        return RouteDecision(intent="other", tool_args={}, clarification="请输入内容。")

    if _looks_like_reminder(text):
        medicine_name = _parse_medicine_name(text)
        time_of_day = _parse_time_of_day(text)

        if not medicine_name and not time_of_day:
            return RouteDecision(
                intent="spring.plan.create.preview",
                tool_args={},
                need_confirm=False,
                clarification="你想提醒的药名和时间是什么？例如：阿司匹林 每天 20:00。",
            )
        if not medicine_name:
            return RouteDecision(
                intent="spring.plan.create.preview",
                tool_args={},
                need_confirm=False,
                clarification="你想提醒的是哪种药？例如：阿司匹林。",
            )
        if not time_of_day:
            return RouteDecision(
                intent="spring.plan.create.preview",
                tool_args={},
                need_confirm=False,
                clarification="你希望每天几点提醒？例如：20:00。",
            )

        # Minimal PlanCreateDTO mapping
        tool_args: Dict[str, Any] = {
            "user_id": user_id,
            "medicineName": medicine_name,
            "dosage": None,
            "startDate": date.today().isoformat(),
            "endDate": None,
            "timePoints": [time_of_day],
            "remark": None,
        }

        return RouteDecision(
            intent="spring.plan.create.preview",
            tool_args=tool_args,
            need_confirm=True,
        )

    return RouteDecision(intent="rag.query", tool_args={"question": text}, need_confirm=False)
