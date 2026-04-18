# 工具执行实时反馈功能

## 功能概述

实现了 AI 工具调用的实时状态推送功能，前端可以显示工具执行的详细步骤和进度，提升用户体验。

## 技术架构

### 后端组件

#### 1. ToolExecutionListener 接口
**位置**: `agent/langchain4j/core/listener/ToolExecutionListener.java`

工具执行监听器接口，定义了三个回调方法：
- `onToolStart`: 工具开始执行时调用
- `onToolSuccess`: 工具执行成功时调用
- `onToolError`: 工具执行失败时调用

#### 2. ToolExecutionBroadcaster 广播器
**位置**: `agent/langchain4j/core/listener/ToolExecutionBroadcaster.java`

核心组件，负责：
- 管理所有 SSE 连接（sessionId -> SseEmitter）
- 接收监听器事件并通过 SSE 推送给前端
- 使用 ThreadLocal 传递 sessionId 上下文

#### 3. ToolExecutionContext 上下文
**位置**: `agent/langchain4j/core/listener/ToolExecutionContext.java`

使用 ThreadLocal 存储当前会话 ID，确保在多线程环境下正确传递 sessionId。

#### 4. ToolExecutionWrapper 包装器
**位置**: `agent/langchain4j/core/listener/ToolExecutionWrapper.java`

工具执行包装器，简化监听器调用：
```java
wrapper.execute("toolName", args, "描述", () -> {
    // 实际工具逻辑
    return result;
});
```

### 前端组件

#### 1. ToolSteps 组件
**位置**: `pages/ai/components/ToolSteps.vue`

显示工具执行步骤的 UI 组件，支持三种状态：
- ⏳ 等待中（pending）
- 🔄 执行中（processing）
- ✅ 成功（success）
- ❌ 失败（error）

#### 2. ChatMessage 组件增强
**位置**: `pages/ai/components/ChatMessage.vue`

新增 `toolSteps` prop，用于在消息气泡中显示工具执行步骤。

#### 3. Assistant.vue 增强
**位置**: `pages/ai/Assistant.vue`

在 `chatStream` 调用中添加 `onToolStatus` 回调：
```javascript
onToolStatus: (toolStatus) => {
    if (toolStatus.type === 'tool_start') {
        // 添加新步骤
        toolSteps.push({...});
    } else if (toolStatus.type === 'tool_complete') {
        // 更新步骤状态
        step.status = toolStatus.status;
    }
}
```

#### 4. agent.js API 增强
**位置**: `api/agent.js`

新增 SSE 事件监听：
```javascript
eventSource.addEventListener('tool_status', (event) => {
    const toolStatus = JSON.parse(event.data);
    if (data.onToolStatus) {
        data.onToolStatus(toolStatus);
    }
});
```

## 使用示例

### 后端：在工具中添加状态推送

以 `PlanCreateTool` 为例：

```java
@Component
public class PlanCreateTool {
    
    @Autowired
    private ToolExecutionBroadcaster broadcaster;
    
    @Tool("创建用药计划")
    public String createPlan(...) {
        return wrapper.execute("createPlan", 
            Map.of("userId", userId, "medicineName", medicineName, ...),
            "创建用药计划",
            () -> {
                // 实际的工具逻辑
                return "计划创建成功";
            }
        );
    }
}
```

### 前端：接收并显示工具状态

在 `Assistant.vue` 的 `sendMessage` 方法中：

```javascript
await agentApi.chatStream({
    user_id: this.userId,
    session_id: this.sessionId,
    message: messageToSend,
    onChunk: (chunk) => {
        // 处理文本流
    },
    onToolStatus: (toolStatus) => {
        // 处理工具状态
        if (toolStatus.type === 'tool_start') {
            toolSteps.push({
                tool_name: toolStatus.tool_name,
                description: toolStatus.description,
                status: 'processing'
            });
        } else if (toolStatus.type === 'tool_complete') {
            // 更新步骤状态
        }
    }
});
```

## SSE 事件格式

### 工具开始执行
```json
{
    "type": "tool_start",
    "tool_name": "createPlan",
    "description": "创建用药计划",
    "status": "processing",
    "args": {
        "userId": "123",
        "medicineName": "阿司匹林"
    }
}
```

### 工具执行完成
```json
{
    "type": "tool_complete",
    "tool_name": "createPlan",
    "status": "success",
    "result": "计划创建成功"
}
```

### 工具执行失败
```json
{
    "type": "tool_complete",
    "tool_name": "createPlan",
    "status": "error",
    "error": "药品不存在"
}
```

## 执行流程

1. **用户发送消息** → 前端调用 `chatStream`
2. **后端注册 SSE** → `ToolExecutionBroadcaster.registerEmitter(sessionId, emitter)`
3. **设置会话上下文** → `ToolExecutionContext.setCurrentSessionId(sessionId)`
4. **AI 调用工具** → LangChain4j 执行 Tool
5. **工具包装器拦截** → `ToolExecutionWrapper.execute()`
6. **推送开始事件** → `broadcaster.onToolStart()`
7. **执行工具逻辑** → 实际业务代码
8. **推送完成事件** → `broadcaster.onToolSuccess()` 或 `onToolError()`
9. **前端接收并显示** → `onToolStatus` 回调更新 UI
10. **清理会话上下文** → `ToolExecutionContext.clearCurrentSessionId()`

## 优势

1. ✅ **非侵入式**: 通过包装器模式，不破坏原有工具代码结构
2. ✅ **通用性**: 所有工具都可以使用相同的包装器
3. ✅ **实时性**: 基于 SSE 实现，毫秒级推送
4. ✅ **可扩展**: 可以轻松添加更多监听器功能（如重试、日志等）
5. ✅ **用户体验**: 实时显示执行进度，给用户充分的反馈

## 注意事项

1. **ThreadLocal 清理**: 必须在请求结束时调用 `clearCurrentSession()`，防止内存泄漏
2. **SSE 连接管理**: 请求结束时必须调用 `unregisterEmitter()` 移除 emitter
3. **异常处理**: 工具执行异常会被捕获并推送给前端，不会中断整个流程
4. **性能考虑**: 每个工具调用都会推送状态，频繁调用可能影响性能

## 未来扩展

1. **步骤详情**: 支持显示更详细的步骤信息（如执行时间、参数等）
2. **进度条**: 对于耗时操作，可以推送进度百分比
3. **用户交互**: 支持在工具执行过程中暂停、取消等操作
4. **批量执行**: 支持多个工具并行执行的状态显示
