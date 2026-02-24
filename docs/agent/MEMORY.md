# 方案A：Spring Boot 负责 Memory（v0）

## 背景

方案A的核心：

- **Spring Boot 作为统一入口与业务核心**，负责 memory 的持久化与确认动作状态。
- Flask 作为 AI 能力（RAG 等），保持尽量“无状态”。

因此：

- 客户端调用：`POST /api/agent/chat`、`POST /api/agent/confirm`
- Spring 内部：
  - 写入/读取 memory（MySQL）
  - 规则路由（提醒/闹钟 → 需要二次确认；否则走 RAG）
  - RAG：调用 Flask `POST /rag/query`
  - 写操作确认：调用 Spring 自己的 `PlanService.createPlan`（写入业务表）

## v0 范围

v0 只做“能跑通链路”的 memory：

- `agent_messages`：保存 user/assistant 消息
- `agent_pending_actions`：保存待确认动作（preview/tool_args/status/过期时间）
- `agent_sessions`：会话记录（预留 summary 字段，v0 不生成摘要）

不做：

- 语义检索/embedding memory
- 自动摘要生成
- 长期结构化事实库（过敏史/偏好等）

## 数据库与建表

默认复用现有业务库 `MedicalAssistant`（即 Spring `spring.datasource.url` 指向的库）。

建表脚本：

- `backend/src/main/resources/db/agentMemory.sql`

执行示例：

```bash
mysql -uroot -p MedicalAssistant < backend/src/main/resources/db/agentMemory.sql
```

## 关键代码

- `com.whu.medicalbackend.agent.memory.AgentMemoryRepository`
  - 基于 `JdbcTemplate` 的最小读写封装
- `com.whu.medicalbackend.agent.AgentOrchestratorService`
  - chat/confirm 的编排逻辑（写入 memory + 调用 Flask RAG + 二次确认写库）
- `com.whu.medicalbackend.agent.flask.FlaskRagProxyService`
  - 调用 Flask `/rag/query`
- `com.whu.medicalbackend.agent.router.AgentRouter`
  - 规则路由（与 Flask 轻量版本一致的思路）

## 接口行为（简述）

### POST /api/agent/chat
- 入参：`user_id`、`session_id`、`message`（可选 `with_trace/with_timing`）
- 行为：写入 user 消息 → 路由 →
  - `rag.query`：调用 Flask /rag/query → 写入 assistant 消息
  - `spring.plan.create.preview`：写入 pending_action（300s 过期）+ 返回 need_confirm=true

### POST /api/agent/confirm
- 入参：`user_id`、`session_id`、`action_id`、`confirm`
- 行为：校验 pending_action（存在/归属/未过期/状态 pending）→
  - confirm=false：标记 canceled
  - confirm=true：调用 `PlanService.createPlan` 写入业务表 → 标记 done/failed

## 后续演进（方向）

- 增加摘要：当消息超过阈值时写入 `agent_sessions.summary_text`
- 增加长期事实表：如 `agent_facts`（带 source/状态可撤销）
- 增加语义记忆：对 message/summary 做 embedding 并检索
