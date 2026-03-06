# 多轮 Function Call 引擎使用指南

## 1. 架构概述

### 1.1 核心组件

```
Agent Controller
    │
    ├──► SimpleMultiTurnEngine (多轮执行引擎)
    │       │
    │       ├──► LLM Service (阿里云 Qwen)
    │       │
    │       └──► Tool Handlers
    │               ├──► PlanTools
    │               ├──► RagTools
    │               └──► WebSearchTools
    │
    └──► AgentOrchestratorService (协调器)
```

### 1.2 执行流程

```
用户："删除我所有的用药计划"
    │
    ▼
第 1 轮：LLM 调用 spring_plan_query
    │
    ▼
引擎：获取计划列表 (2 个计划)
    │
    ▼
第 2 轮：LLM 调用 spring_plan_delete(planId=1)
    │
    ▼
第 3 轮：LLM 调用 spring_plan_delete(planId=2)
    │
    ▼
引擎：返回最终结果
```

## 2. 快速开始

### 2.1 启用多轮执行

在 `application.yml` 中配置：

```yaml
agent:
  llm:
    enabled: true  # 启用 LLM
  multi-turn:
    enabled: true  # 启用多轮执行
    max-turns: 5   # 最大执行轮数
```

### 2.2 测试场景

#### 场景 1：删除单个计划

**请求：**
```json
POST /api/agent/chat
{
  "user_id": "1",
  "session_id": "test-session-1",
  "message": "删除阿司匹林用药计划",
  "with_trace": true,
  "with_timing": false
}
```

**预期流程：**
1. LLM 识别意图 → 调用 `spring_plan_delete` with `medicineName: "阿司匹林"`
2. PlanTools 查找匹配的计划
3. 删除所有匹配的计划
4. 返回结果

**响应：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "assistant_message": "已成功删除 1 个与\"阿司匹林\"相关的用药计划",
    "deleted_count": 1,
    "trace": {
      "tools_called": ["spring_plan_delete"],
      "execution_turns": 1
    }
  }
}
```

#### 场景 2：删除所有计划（多轮执行）

**请求：**
```json
POST /api/agent/chat
{
  "user_id": "1",
  "session_id": "test-session-2",
  "message": "删除我所有的用药计划",
  "with_trace": true,
  "with_timing": true
}
```

**预期流程：**
1. 第 1 轮：LLM 调用 `spring_plan_query` → 获取计划列表
2. 第 2 轮：LLM 调用 `spring_plan_delete(planId=1)` → 删除计划 1
3. 第 3 轮：LLM 调用 `spring_plan_delete(planId=2)` → 删除计划 2
4. 引擎返回最终结果

**响应：**
```json
{
  "code": 200,
  "data": {
    "success": true,
    "assistant_message": "已成功删除你所有的 2 个用药计划",
    "deleted_count": 2,
    "trace": {
      "tools_called": ["spring_plan_query", "spring_plan_delete", "spring_plan_delete"],
      "execution_turns": 3,
      "timings": {
        "total_ms": 1523
      }
    }
  }
}
```

## 3. 工具开发指南

### 3.1 新增工具

**步骤 1：在 PlanTools 中添加处理方法**

```java
public Map<String, Object> executeTool(...) {
    return switch (toolName) {
        // ... 现有工具
        case "spring_plan_batch_create" -> handleBatchCreate(...);
        default -> null;
    };
}

private Map<String, Object> handleBatchCreate(...) {
    // 实现批量创建逻辑
    List<PlanCreateDTO> plans = ...;
    
    int createdCount = 0;
    for (PlanCreateDTO plan : plans) {
        planService.createPlan(userId, plan);
        createdCount++;
    }
    
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("success", true);
    result.put("created_count", createdCount);
    result.put("assistant_message", "已成功创建 " + createdCount + " 个用药计划");
    
    return result;
}
```

**步骤 2：添加工具定义**

```java
public ObjectNode createPlanBatchCreateTool() {
    ObjectNode function = objectMapper.createObjectNode();
    function.put("type", "function");
    
    ObjectNode funcDef = objectMapper.createObjectNode();
    funcDef.put("name", "spring_plan_batch_create");
    funcDef.put("description", "批量创建多个用药计划");
    
    ObjectNode parameters = objectMapper.createObjectNode();
    parameters.put("type", "object");
    
    ObjectNode properties = objectMapper.createObjectNode();
    
    ObjectNode plans = objectMapper.createObjectNode();
    plans.put("type", "array");
    plans.put("description", "要创建的计划列表");
    properties.set("plans", plans);
    
    parameters.set("properties", properties);
    funcDef.set("parameters", parameters);
    function.set("function", funcDef);
    
    return function;
}
```

**步骤 3：注册到工具列表**

```java
public ArrayNode getAllPlanTools() {
    ArrayNode tools = objectMapper.createArrayNode();
    tools.add(createPlanQueryTool());
    tools.add(createPlanUpdateTool());
    tools.add(createPlanDeleteTool());
    tools.add(createPlanCreateTool());
    tools.add(createPlanBatchCreateTool()); // 新增
    return tools;
}
```

### 3.2 支持多轮执行的工具

如果工具需要多轮执行，返回 `need_continue: true`：

```java
private Map<String, Object> handleComplexTask(...) {
    // 第一轮：查询数据
    if (step == 1) {
        List<Data> dataList = queryData();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", dataList);
        result.put("assistant_message", "已查询到 " + dataList.size() + " 条数据");
        result.put("need_continue", true);  // 需要继续
        result.put("next_step_suggestion", "继续处理这些数据");
        
        return result;
    }
    
    // 第二轮：处理数据
    else {
        processData();
        
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("assistant_message", "数据处理完成");
        result.put("need_continue", false);  // 完成
        
        return result;
    }
}
```

## 4. 调试技巧

### 4.1 查看执行日志

```bash
# 查看多轮执行日志
tail -f logs/application.log | grep "多轮执行引擎"

# 查看 LLM 调用日志
tail -f logs/application.log | grep "LLM 结果"

# 查看工具调用日志
tail -f logs/application.log | grep "调用工具"
```

### 4.2 使用 Postman 测试

**导入测试集合：**

1. 打开 Postman
2. 导入 `postman/Agent_MultiTurn_Tests.json`
3. 设置环境变量 `baseUrl = http://localhost:8080`
4. 运行测试

**手动测试：**

```bash
# 测试删除所有计划
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "1",
    "session_id": "test-123",
    "message": "删除我所有的用药计划",
    "with_trace": true
  }'
```

### 4.3 常见问题

**问题 1：LLM 没有调用预期的工具**

**解决：**
- 检查系统提示中的工具描述是否清晰
- 在 `AgentPromptTemplate.java` 中添加更多示例
- 调整 LLM 的 temperature 参数（降低随机性）

**问题 2：多轮执行陷入死循环**

**解决：**
- 检查 `MAX_TURNS` 配置（默认 5 轮）
- 确保工具返回 `need_continue: false` 时结束
- 添加超时机制

**问题 3：工具执行失败**

**解决：**
- 查看日志中的详细错误信息
- 检查工具参数是否正确
- 验证数据库连接和权限

## 5. 性能优化

### 5.1 并发执行

对于独立的任务，可以并发执行：

```java
// 并发删除多个计划
List<Long> planIds = ...;
planIds.parallelStream().forEach(planId -> {
    planService.deletePlan(userId, planId);
});
```

### 5.2 缓存优化

```java
@Cacheable(value = "plans", key = "#userId")
public List<PlanVO> getPlanList(Long userId) {
    return planMapper.findByUserId(userId);
}
```

### 5.3 批量操作

```java
// 批量删除，而不是逐个删除
@Transactional
public void deletePlans(Long userId, List<Long> planIds) {
    planMapper.batchDelete(userId, planIds);
    taskMapper.batchDeleteByPlanIds(planIds);
}
```

## 6. 监控与告警

### 6.1 关键指标

```java
// 在 SimpleMultiTurnEngine 中添加指标收集
private final AtomicInteger executionCount = new AtomicInteger(0);
private final AtomicInteger errorCount = new AtomicInteger(0);
private final AverageTimer executionTime = new AverageTimer();

public Map<String, Object> execute(...) {
    executionCount.incrementAndGet();
    long startTime = System.currentTimeMillis();
    
    try {
        // 执行逻辑
    } catch (Exception e) {
        errorCount.incrementAndGet();
        throw e;
    } finally {
        executionTime.record(System.currentTimeMillis() - startTime);
    }
}
```

### 6.2 告警规则

```yaml
# Prometheus 告警规则
groups:
  - name: agent_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(agent_execution_errors_total[5m]) > 0.1
        annotations:
          summary: "Agent 错误率过高"
          
      - alert: SlowExecution
        expr: agent_execution_time_seconds > 5
        annotations:
          summary: "Agent 执行时间过长"
```

## 7. 最佳实践

### 7.1 工具设计原则

1. **单一职责**：每个工具只做一件事
2. **幂等性**：工具可以安全地重复调用
3. **错误处理**：捕获所有异常，返回友好错误消息
4. **日志记录**：记录关键步骤和错误信息

### 7.2 多轮执行策略

1. **限制轮数**：设置合理的 `MAX_TURNS`（建议 3-5 轮）
2. **明确终止条件**：每轮都应该判断是否需要继续
3. **上下文管理**：保持对话历史的连贯性
4. **回退机制**：多轮失败时回退到单轮或规则匹配

### 7.3 用户体验

1. **进度提示**：告诉用户当前执行到哪一步
2. **错误恢复**：提供重试或回退选项
3. **结果确认**：重要操作需要用户确认
4. **性能感知**：长时间操作时显示加载动画

## 8. 扩展阅读

- [多轮 Function Call 设计文档](./MULTI_TURN_ENGINE_DESIGN.md)
- [AgentOrchestratorService 源码](./src/main/java/com/whu/medicalbackend/agent/AgentOrchestratorService.java)
- [SimpleMultiTurnEngine 源码](./src/main/java/com/whu/medicalbackend/agent/engine/SimpleMultiTurnEngine.java)
- [PlanTools 源码](./src/main/java/com/whu/medicalbackend/agent/tools/PlanTools.java)

## 9. 总结

多轮 Function Call 引擎的核心优势：

✅ **自动化多步操作**：无需用户手动触发多次
✅ **解耦设计**：工具与引擎分离，易于维护
✅ **可扩展**：新增工具无需修改引擎
✅ **健壮**：完善的错误处理和回退机制
✅ **可观测**：完整的执行追踪和日志

通过合理使用多轮执行引擎，可以大大提升用户体验和系统智能化水平！
