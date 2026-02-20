# Agent架构设计文档

## 1. 架构概述

MedicalAssistant项目采用了双端Agent架构设计，包括Flask端和Spring Boot端，两者共享相同的设计理念和接口规范，确保服务的一致性和可替代性。

### 核心设计理念

- **模块化**：将Agent核心逻辑、工具集成、记忆管理等功能模块化，提高代码可维护性
- **可扩展**：通过工具注册机制，支持动态添加新工具，无需修改核心逻辑
- **统一接口**：Flask端和Spring Boot端保持相同的API接口，确保前端调用不受影响
- **记忆增强**：实现会话记忆和待确认操作管理，提升用户体验
- **智能路由**：基于规则的意图识别和路由，支持多轮对话

## 2. Flask端Agent结构

### 目录结构

```
app/services/agent/
├── __init__.py            # 模块初始化
├── orchestrator.py        # 核心协调器
├── router.py              # 意图路由器
├── memory/                # 记忆管理
│   ├── __init__.py
│   └── sqlite_memory.py   # SQLite记忆实现
├── tools/                 # 工具集成
│   ├── __init__.py
│   ├── registry.py        # 工具注册器
│   ├── rag_tool.py        # RAG查询工具
│   └── spring_tool.py     # Spring计划工具
└── utils/                 # 通用工具
    └── __init__.py
```

### 核心组件

1. **AgentOrchestrator**：核心协调器，处理消息流转和工具调用
2. **Router**：意图路由器，基于规则识别用户意图
3. **SQLiteMemory**：记忆管理器，基于SQLite存储会话数据
4. **ToolRegistry**：工具注册器，管理和调用工具
5. **Tools**：工具集，包括RAG查询和Spring计划创建

### 消息处理流程

1. 接收用户消息，保存到记忆中
2. 调用Router识别用户意图
3. 根据意图调用相应工具
4. 处理工具返回结果
5. 生成助手回复，保存到记忆中
6. 返回结果给前端

## 3. Spring Boot端Agent结构

### 目录结构

```
com/whu/medicalbackend/agent/
├── AgentOrchestratorService.java  # 核心协调器
├── flask/                         # Flask代理
│   └── FlaskRagProxyService.java  # RAG代理服务
├── memory/                        # 记忆管理
│   └── AgentMemoryRepository.java # 记忆仓库
├── router/                        # 意图路由器
│   └── AgentRouter.java           # 路由实现
└── tools/                         # 工具集成
    └── ToolRegistry.java          # 工具注册器
```

### 核心组件

1. **AgentOrchestratorService**：核心协调器，处理消息流转和工具调用
2. **AgentRouter**：意图路由器，基于规则识别用户意图
3. **AgentMemoryRepository**：记忆管理器，基于MySQL存储会话数据
4. **ToolRegistry**：工具注册器，管理和调用工具
5. **FlaskRagProxyService**：RAG代理服务，调用Flask端的RAG服务

### 消息处理流程

与Flask端相同，保持一致的处理逻辑：
1. 接收用户消息，保存到记忆中
2. 调用Router识别用户意图
3. 根据意图调用相应工具
4. 处理工具返回结果
5. 生成助手回复，保存到记忆中
6. 返回结果给前端

## 4. 工具集成机制

### 工具注册

Flask端和Spring Boot端都实现了ToolRegistry，支持动态注册和管理工具：

```python
# Flask端工具注册
tool_registry.register(
    "rag.query",
    rag_query_tool,
    description="Query RAG service for medical information",
    params_schema={"question": "string", "with_trace": "boolean", "with_timing": "boolean"}
)
```

```java
// Spring Boot端工具注册
toolRegistry.register("rag.query", "Query RAG service for medical information", params -> {
    String question = str(params.get("question"));
    boolean withTrace = bool(params.get("with_trace"));
    boolean withTiming = bool(params.get("with_timing"));
    return flaskRagProxyService.query(question, withTrace, withTiming);
});
```

### 工具调用

使用注册器获取工具，支持fallback机制：

```python
# Flask端工具调用
tool_info = registry.get("rag.query")
if tool_info:
    result = tool_info["function"](**params)
else:
    result = default_function(**params)
```

```java
// Spring Boot端工具调用
ToolRegistry.ToolInfo toolInfo = toolRegistry.get("rag.query");
if (toolInfo != null) {
    Map<String, Object> result = toolInfo.function().apply(params);
} else {
    Map<String, Object> result = defaultFunction(params);
}
```

## 5. 记忆管理

### Flask端记忆管理

- **存储方式**：SQLite数据库（agent.sqlite3）
- **存储内容**：会话信息、消息历史、待确认操作
- **主要功能**：
  - 追加消息到会话
  - 保存待确认操作
  - 获取待确认操作
  - 更新操作状态
  - 支持操作过期机制

### Spring Boot端记忆管理

- **存储方式**：MySQL数据库
- **存储表**：agent_sessions、agent_messages、agent_pending_actions
- **主要功能**：
  - 与Flask端相同的记忆管理功能
  - 支持更复杂的查询和事务处理
  - 与Spring Boot生态集成

## 6. 接口设计

### API接口

Flask端和Spring Boot端提供相同的API接口：

1. **POST /agent/chat**：处理用户消息
   - 参数：user_id, session_id, message, with_trace, with_timing
   - 返回：assistant_message, need_confirm, actions, trace, timings

2. **POST /agent/confirm**：确认待执行操作
   - 参数：user_id, session_id, action_id, confirm
   - 返回：success, assistant_message, actions

3. **GET /agent/health**：健康检查
   - 返回：状态信息

### 响应格式

```json
{
  "success": true,
  "assistant_message": "助手回复内容",
  "need_confirm": false,
  "actions": [],
  "trace": {
    "agent_version": "v1_light_rules_2026-02-19",
    "decision": {
      "intent": "rag.query",
      "need_confirm": false
    },
    "tools_called": ["rag.query"]
  },
  "timings": {
    "route": 0.001,
    "tool.rag.query": 1.234
  }
}
```

## 7. 配置说明

### Flask端配置

通过环境变量配置：

```bash
# 服务配置
FLASK_HOST=0.0.0.0
FLASK_PORT=8001
FLASK_DEBUG=false

# RAG配置
RAG_PROXY_URL=http://localhost:8001/rag/query
RAG_LOCAL_MODE=true

# 模型配置
OPENAI_API_KEY=your_api_key
OPENAI_MODEL=gpt-3.5-turbo
```

### Spring Boot端配置

通过application.yaml配置：

```yaml
# Flask AI service
flask:
  base-url: http://127.0.0.1:8001
  timeout-ms: 120000

# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/MedicalAssistant
    username: root
    password: 1234
```

## 8. 扩展指南

### 添加新工具

1. **创建工具文件**：在 `tools` 目录下创建新的工具文件
2. **实现工具函数**：定义工具函数，处理具体业务逻辑
3. **注册工具**：在 `orchestrator.py` 中注册新工具
4. **更新路由**：在 `router.py` 中添加新意图的路由规则

### 示例：添加药物相互作用检查工具

```python
# tools/drug_interaction_tool.py
def drug_interaction_tool(drugs, with_trace=False, with_timing=False):
    # 实现药物相互作用检查逻辑
    return {"success": True, "interactions": []}

# orchestrator.py 中注册工具
if not registry.has("drug.interaction"):
    registry.register(
        "drug.interaction",
        drug_interaction_tool,
        description="Check drug interactions",
        params_schema={"drugs": "array", "with_trace": "boolean", "with_timing": "boolean"}
    )

# router.py 中添加路由规则
if "药物相互作用" in message:
    return RouteDecision("drug.interaction", {"drugs": extract_drugs(message)}, False, None)
```

### 增强意图识别

1. **扩展Router**：在 `router.py` 中添加新的意图识别规则
2. **集成NLP模型**：可以集成更先进的NLP模型，提高意图识别准确性
3. **上下文感知**：考虑会话历史，实现更智能的意图识别

## 9. 部署建议

### 开发环境

1. **启动Flask服务**：
   ```bash
   cd /root/flask
   pip install -r requirements.txt
   python app.py
   ```

2. **启动Spring Boot服务**：
   ```bash
   cd /root/backend
   mvn clean install
   mvn spring-boot:run
   ```

### 生产环境

1. **容器化部署**：
   - 使用Docker容器化Flask和Spring Boot服务
   - 配置Docker Compose管理多容器部署

2. **负载均衡**：
   - 可以根据需要部署多个Flask或Spring Boot实例
   - 使用Nginx等反向代理进行负载均衡

3. **监控与日志**：
   - 配置Prometheus和Grafana监控服务状态
   - 使用ELK栈收集和分析日志

4. **安全措施**：
   - 配置HTTPS加密传输
   - 实现API密钥认证
   - 限制请求频率，防止滥用

## 10. 技术栈

### Flask端

- **Python 3.10+**：主要开发语言
- **Flask**：Web框架
- **SQLite**：轻量级数据库，用于记忆管理
- **LangChain**：RAG集成（可选）

### Spring Boot端

- **Java 17+**：主要开发语言
- **Spring Boot 3.2+**：Web框架
- **MySQL**：关系型数据库，用于记忆管理
- **MyBatis**：ORM框架

### 共享技术

- **RESTful API**：服务间通信
- **JSON**：数据交换格式
- **工具注册模式**：动态工具管理
- **规则引擎**：意图识别和路由

## 11. 未来发展

### 技术演进路线

1. **智能意图识别**：集成更先进的NLP模型，提高意图识别准确性
2. **多模态支持**：添加图像和语音处理能力，支持更丰富的用户输入
3. **个性化推荐**：基于用户历史行为，提供个性化的医疗建议
4. **知识图谱**：集成医疗知识图谱，提高回答的准确性和权威性
5. **自动工具发现**：实现工具的自动发现和注册，进一步提高扩展性

### 功能扩展

1. **健康监测**：集成可穿戴设备数据，提供健康监测和预警
2. **医疗预约**：支持在线医疗预约和管理
3. **用药管理**：更复杂的用药计划和提醒功能
4. **健康报告**：自动生成健康报告和建议
5. **多语言支持**：添加多语言能力，服务更广泛的用户群体

## 12. 总结

MedicalAssistant的Agent架构设计采用了模块化、可扩展的设计理念，通过双端实现确保服务的可靠性和可替代性。该架构不仅满足了当前的功能需求，也为未来的扩展和演进预留了空间。

通过工具注册机制、记忆管理、智能路由等核心功能，Agent能够提供更加智能、个性化的医疗助手服务，帮助用户更好地管理健康。

---

**版本**: v1.0.0
**最后更新**: 2026-02-20
**维护者**: MedicalAssistant团队
