# Agent 架构说明

## 目录结构

```
agent/
├── README.md                          # 本文档
├── AgentOrchestratorService.java      # 统一编排入口
├── core/                              # 核心基础组件
│   └── memory/
│       └── AgentMemoryRepository.java  # 对话记忆存储（MySQL）
├── flask/                             # Flask 后端服务代理
│   └── FlaskRagProxyService.java       # RAG 查询代理
└── langchain4j/                       # LangChain4j 框架实现
    ├── config/                        # 配置类
    │   └── LangChain4jConfig.java       # ChatModel 配置
    ├── agents/                        # 具体 Agent 实现
    │   └── MedicalAgent.java            # 医疗助手 Agent
    └── tools/                         # 工具集合
        ├── PlanQueryTool.java            # 查询用药计划
        ├── PlanCreateTool.java           # 创建用药计划
        ├── PlanUpdateTool.java           # 更新用药计划
        └── PlanDeleteTool.java           # 删除用药计划
```

## 模块职责

### 1. **入口层** - `AgentOrchestratorService.java`
- **职责**：统一请求入口，路由到不同的 Agent 实现
- **功能**：
  - 解析请求参数
  - 管理对话记忆
  - 选择并调用合适的 Agent
  - 回退机制（Agent 失败时使用简单 LLM）

### 2. **核心层** - `core/`
- **职责**：提供通用的基础能力，不依赖具体框架
- **memory/**：对话记忆存储
  - 使用 MySQL 存储会话和消息历史
  - 支持 pending actions（待确认操作）

### 3. **Flask 代理层** - `flask/`
- **职责**：代理调用 Flask 后端的服务
- **FlaskRagProxyService**：调用 Flask 的 RAG 查询接口

### 4. **LangChain4j 框架层** - `langchain4j/`
- **职责**：基于 LangChain4j 框架的 Agent 实现
- **config/**：框架配置
  - `LangChain4jConfig`：配置 DashScope ChatModel（千问）
- **agents/**：具体的 Agent 实现
  - `MedicalAgent`：医疗健康助手，支持用药计划管理
- **tools/**：可供 Agent 调用的工具
  - 所有工具使用 `@Tool` 注解标注
  - 工具参数使用 `@P` 注解标注

## 扩展指南

### 添加新的 Agent

1. 在 `langchain4j/agents/` 下创建新的 Agent 类
2. 使用 `@Component` 注解
3. 实现 `execute()` 方法
4. 在 `AgentOrchestratorService` 中注入并调用

### 添加新的 Tool

1. 在 `langchain4j/tools/` 下创建新的 Tool 类
2. 使用 `@Component` 注解
3. 使用 `@Tool` 注解标注工具方法
4. 使用 `@P` 注解标注参数
5. 在对应的 Agent 中注入并注册到 AiServices

### 接入 Flask 的其他服务

1. 在 `flask/` 下创建新的 Service 类
2. 使用 `@Service` 注解
3. 注入 `RestClient` 调用 Flask 接口
4. 可作为 Tool 提供给 Agent 使用

## 技术栈

- **LangChain4j**：1.1.0
- **LLM**：阿里云 DashScope（千问）
- **记忆存储**：MySQL
- **框架**：Spring Boot

## 注意事项

- 所有工具返回的 Map 必须使用 `LinkedHashMap`，避免 `Map.of()` 的 null 值限制
- 系统提示词需要明确说明工具使用场景和参数要求
- 用户 ID 通过 `@V("userId")` 注入到系统提示词中
