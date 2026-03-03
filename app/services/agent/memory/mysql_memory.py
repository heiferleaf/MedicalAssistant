import json
import os
import time
from typing import Any, Dict, List, Optional

import pymysql
from pymysql.cursors import DictCursor


class MySQLMemory:
    def __init__(self, *, host: str = "localhost", port: int = 3306, user: str = "root", password: str = "1234", db: str = "MedicalAssistant") -> None:
        self.host = host
        self.port = port
        self.user = user
        self.password = password
        self.db = db
        # 不再创建表结构，使用Spring Boot端的建表语句
        # 确保数据库连接正常
        self._test_connection()

    def _connect(self):
        return pymysql.connect(
            host=self.host,
            port=self.port,
            user=self.user,
            password=self.password,
            db=self.db,
            cursorclass=DictCursor,
            charset='utf8mb4',
            autocommit=False
        )

    def _test_connection(self):
        """测试数据库连接是否正常"""
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute("SELECT 1")
                cursor.fetchone()

    def _ensure_schema(self) -> None:
        """保留方法名以保持兼容性，但不再创建表结构"""
        pass

    def _touch_session(self, *, session_id: str, user_id: str) -> None:
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT session_id FROM agent_sessions WHERE session_id=%s",
                    (session_id,)
                )
                row = cursor.fetchone()
                if row is None:
                    cursor.execute(
                        "INSERT INTO agent_sessions(session_id,user_id,summary_text) VALUES(%s,%s,%s)",
                        (session_id, user_id, None)
                    )
            conn.commit()

    def append_message(self, *, session_id: str, user_id: str, role: str, content: str) -> None:
        self._touch_session(session_id=session_id, user_id=user_id)
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "INSERT INTO agent_messages(session_id,user_id,role,content) VALUES(%s,%s,%s,%s)",
                    (session_id, user_id, role, content)
                )
            conn.commit()

    def get_recent_messages(self, *, session_id: str, limit: int = 10) -> List[Dict[str, Any]]:
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT role, content, created_at FROM agent_messages WHERE session_id=%s ORDER BY id DESC LIMIT %s",
                    (session_id, limit)
                )
                rows = cursor.fetchall()
        # return in chronological order
        out = rows[::-1]  # Reverse to get chronological order
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
        import datetime
        expires_at = datetime.datetime.now() + datetime.timedelta(seconds=expires_in_sec)
        expires_at_str = expires_at.strftime('%Y-%m-%d %H:%M:%S')
        
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    """
                    INSERT INTO agent_pending_actions(
                        action_id, session_id, user_id, action_type,
                        preview_json, tool_args_json, status, result_json,
                        expires_at
                    ) VALUES(%s,%s,%s,%s,%s,%s,%s,%s,%s)
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
                        expires_at_str,
                    ),
                )
            conn.commit()

    def get_pending_action(self, action_id: str) -> Optional[Dict[str, Any]]:
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "SELECT * FROM agent_pending_actions WHERE action_id=%s",
                    (action_id,)
                )
                row = cursor.fetchone()
                if row is None:
                    return None

                import datetime
                now = datetime.datetime.now()
                expires_at = row["expires_at"]
                
                if isinstance(expires_at, str):
                    expires_at = datetime.datetime.strptime(expires_at, '%Y-%m-%d %H:%M:%S')
                
                if expires_at < now:
                    cursor.execute("DELETE FROM agent_pending_actions WHERE action_id=%s", (action_id,))
                    conn.commit()
                    return None

                out = dict(row)
                out["preview"] = json.loads(out.get("preview_json") or "{}")
                out["tool_args"] = json.loads(out.get("tool_args_json") or "{}")
                out["result"] = json.loads(out.get("result_json") or "{}") if out.get("result_json") else None
                return out

    def update_pending_action_status(self, action_id: str, *, status: str, result: Optional[Dict[str, Any]] = None) -> None:
        with self._connect() as conn:
            with conn.cursor() as cursor:
                cursor.execute(
                    "UPDATE agent_pending_actions SET status=%s, result_json=%s WHERE action_id=%s",
                    (status, json.dumps(result, ensure_ascii=False) if result is not None else None, action_id),
                )
            conn.commit()
