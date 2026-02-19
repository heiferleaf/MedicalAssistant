import json
import os
import sqlite3
import time
from typing import Any, Dict, List, Optional


DEFAULT_DB_PATH = os.getenv("AGENT_DB_PATH", "/root/flask/agent.sqlite3")


class SQLiteMemory:
    def __init__(self, *, db_path: str = DEFAULT_DB_PATH) -> None:
        self.db_path = db_path
        self._ensure_schema()

    def _connect(self) -> sqlite3.Connection:
        conn = sqlite3.connect(self.db_path, timeout=10)
        conn.row_factory = sqlite3.Row
        return conn

    def _ensure_schema(self) -> None:
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        with self._connect() as conn:
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS sessions (
                    session_id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL,
                    summary_text TEXT
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    role TEXT NOT NULL,
                    content TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
                """
            )
            conn.execute(
                """
                CREATE TABLE IF NOT EXISTS pending_actions (
                    action_id TEXT PRIMARY KEY,
                    session_id TEXT NOT NULL,
                    user_id TEXT NOT NULL,
                    action_type TEXT NOT NULL,
                    preview_json TEXT NOT NULL,
                    tool_args_json TEXT NOT NULL,
                    status TEXT NOT NULL,
                    result_json TEXT,
                    created_at INTEGER NOT NULL,
                    expires_at INTEGER NOT NULL
                )
                """
            )

    def _touch_session(self, *, session_id: str, user_id: str) -> None:
        now = int(time.time())
        with self._connect() as conn:
            row = conn.execute(
                "SELECT session_id FROM sessions WHERE session_id=?",
                (session_id,),
            ).fetchone()
            if row is None:
                conn.execute(
                    "INSERT INTO sessions(session_id,user_id,created_at,updated_at,summary_text) VALUES(?,?,?,?,?)",
                    (session_id, user_id, now, now, None),
                )
            else:
                conn.execute(
                    "UPDATE sessions SET updated_at=? WHERE session_id=?",
                    (now, session_id),
                )

    def append_message(self, *, session_id: str, user_id: str, role: str, content: str) -> None:
        self._touch_session(session_id=session_id, user_id=user_id)
        now = int(time.time())
        with self._connect() as conn:
            conn.execute(
                "INSERT INTO messages(session_id,user_id,role,content,created_at) VALUES(?,?,?,?,?)",
                (session_id, user_id, role, content, now),
            )

    def get_recent_messages(self, *, session_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        with self._connect() as conn:
            rows = conn.execute(
                "SELECT role, content, created_at FROM messages WHERE session_id=? ORDER BY id DESC LIMIT ?",
                (session_id, limit),
            ).fetchall()
        # return in chronological order
        out = [dict(r) for r in reversed(rows)]
        return out

    def save_pending_action(
        self,
        *,
        action_id: str,
        session_id: str,
        user_id: str,
        action_type: str,
        preview: Dict[str, Any],
        tool_args: Dict[str, Any],
        expires_in_sec: int,
    ) -> None:
        now = int(time.time())
        expires_at = now + int(expires_in_sec)
        with self._connect() as conn:
            conn.execute(
                """
                INSERT INTO pending_actions(
                    action_id, session_id, user_id, action_type,
                    preview_json, tool_args_json, status, result_json,
                    created_at, expires_at
                ) VALUES(?,?,?,?,?,?,?,?,?,?)
                """,
                (
                    action_id,
                    session_id,
                    user_id,
                    action_type,
                    json.dumps(preview, ensure_ascii=False),
                    json.dumps(tool_args, ensure_ascii=False),
                    "pending",
                    None,
                    now,
                    expires_at,
                ),
            )

    def get_pending_action(self, action_id: str) -> Optional[Dict[str, Any]]:
        now = int(time.time())
        with self._connect() as conn:
            row = conn.execute(
                "SELECT * FROM pending_actions WHERE action_id=?",
                (action_id,),
            ).fetchone()
            if row is None:
                return None

            if int(row["expires_at"]) < now:
                conn.execute("DELETE FROM pending_actions WHERE action_id=?", (action_id,))
                return None

            out = dict(row)
            out["preview"] = json.loads(out.get("preview_json") or "{}")
            out["tool_args"] = json.loads(out.get("tool_args_json") or "{}")
            out["result"] = json.loads(out.get("result_json") or "{}") if out.get("result_json") else None
            return out

    def update_pending_action_status(self, action_id: str, *, status: str, result: Optional[Dict[str, Any]] = None) -> None:
        with self._connect() as conn:
            conn.execute(
                "UPDATE pending_actions SET status=?, result_json=? WHERE action_id=?",
                (status, json.dumps(result, ensure_ascii=False) if result is not None else None, action_id),
            )
