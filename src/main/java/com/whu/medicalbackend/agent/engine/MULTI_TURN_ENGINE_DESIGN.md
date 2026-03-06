# 多轮 Function Call 引擎设计文档

## 1. 问题背景

当前架构存在的问题：
1. **单次 Function Call 限制**：LLM 一次只能调用一个工具，无法完成需要多步操作的任务
2. **耦合严重**：工具执行逻辑与 LLM 调用逻辑混在一起
3. **扩展性差**：新增工具需要修改多处代码

### 典型场景

**场景 1：删除所有用药计划**
```
用户："删除我所有的用药计划"

当前流程（有问题）：
1. LLM 调用 spring_plan_query → 返回 2 个计划
2. LLM 应该继续调用 spring_plan_delete × 2 次，但实际没有

期望流程：
1. LLM 调用 spring_plan_query → 返回 2 个计划
2. 引擎自动继续调用 spring_plan_delete(planId=1)
3. 引擎自动继续调用 spring_plan_delete(planId=2)
4. 返回最终结果
```

## 2. 架构设计

### 2.1 核心组件

```
┌─────────────────────────────────────────────────────┐
│          MultiTurnExecutionEngine                   │
│  (多轮执行引擎 - 核心协调器)                         │
├─────────────────────────────────────────────────────┤
│  - execute()                                        │
│  - callLLM()                                        │
│  - executeTool()                                    │
│  - buildFinalResponse()                             │
└─────────────────────────────────────────────────────┘
           │
           ├──► ToolRegistry (工具注册中心)
           │       - register(toolName, handler)
           │       - execute(toolName, params)
           │
           ├──► LLMClient (LLM 客户端)
           │       - chatWithFunctionCall()
           │
           └──► ExecutionContext (执行上下文)
                    - userId, sessionId
                    - conversationHistory
                    - toolResults
```

### 2.2 数据流

```
用户请求
   │
   ▼
┌─────────────────────┐
│ Agent Controller    │
└─────────────────────┘
   │
   ▼
┌─────────────────────────────────────────┐
│  MultiTurnExecutionEngine.execute()     │
│  ┌─────────────────────────────────┐    │
│  │ Loop (max 5 turns)              │    │
│  │   1. 调用 LLM                   │    │
│  │   2. 解析 Function Call         │    │
│  │   3. 执行工具                   │    │
│  │   4. 判断是否继续               │    │
│  │      - needContinue = true     │    │
│  │      - 添加工具结果到上下文     │    │
│  │      - 继续下一轮               │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
   │
   ▼
最终响应
```

## 3. 接口定义

### 3.1 ToolExecutionResult

```java
public class ToolExecutionResult {
    ExecutionStatus status;      // SUCCESS, CONTINUE, FAILED, NEED_CONFIRM
    String toolName;
    Map<String, Object> data;
    String message;
    boolean needContinue;        // 是否需要继续调用
    String nextStepSuggestion;   // 下一步建议
}
```

### 3.2 工具处理器接口

```java
@FunctionalInterface
public interface ToolHandler {
    ToolExecutionResult execute(
        String userId,
        String sessionId,
        Map<String, Object> params,
        ExecutionContext context
    );
}
```

## 4. 使用示例

### 4.1 注册工具

```java
@Service
public class ToolConfiguration {
    
    @Autowired
    private ToolRegistry toolRegistry;
    
    @Autowired
    private PlanTools planTools;
    
    @PostConstruct
    public void init() {
        // 注册 Plan 相关工具
        toolRegistry.register("spring_plan_delete", (userId, sessionId, params, context) -> {
            // 工具实现
            return planTools.handleDelete(userId, sessionId, params, context);
        });
        
        // 注册 RAG 工具
        toolRegistry.register("rag_query", (userId, sessionId, params, context) -> {
            return ragTools.query(userId, sessionId, params, context);
        });
    }
}
```

### 4.2 工具返回多轮执行信号

```java
public Map<String, Object> handlePlanDelete(...) {
    if (deleteAll) {
        List<PlanVO> plans = planService.getPlanList(uid);
        
        if (plans.isEmpty()) {
            return ToolExecutionResult.success(
                "spring_plan_delete",
                Map.of("deleted_count", 0),
                "你目前没有任何用药计划"
            );
        }
        
        // 返回需要继续执行的信号
        return ToolExecutionResult.continueExecution(
            "spring_plan_delete",
            Map.of("plans_to_delete", plans),
            "正在删除 " + plans.size() + " 个计划...",
            "继续删除剩余计划"
        );
    }
}
```

### 4.3 引擎处理多轮执行

```java
public Map<String, Object> execute(...) {
    for (int turn = 0; turn < MAX_TURNS; turn++) {
        // 1. 调用 LLM
        LLMResponse llmResponse = callLLM(...);
        
        // 2. 执行工具
        ToolExecutionResult result = executeTool(...);
        
        // 3. 检查是否需要继续
        if (!result.isNeedContinue()) {
            return buildFinalResponse(result);
        }
        
        // 4. 添加工具结果到对话历史
        context.addToolResult(result.getToolName(), result.toJson());
        
        // 5. 继续下一轮
    }
}
```

## 5. 扩展性设计

### 5.1 新增工具

**步骤 1：创建工具类**
```java
@Component
public class NewTools {
    public ToolExecutionResult executeNewTool(...) {
        // 实现
    }
}
```

**步骤 2：注册工具**
```java
toolRegistry.register("new_tool", newTools::executeNewTool);
```

**步骤 3：添加工具定义到 LLM**
```java
ToolDefinition.createTool(
    "new_tool",
    "工具描述",
    Map.of("param1", "type", "param2", "type")
);
```

### 5.2 自定义执行策略

```java
public interface ExecutionStrategy {
    boolean shouldContinue(ToolExecutionResult result, int currentTurn);
    void onBeforeTurn(int turn, ExecutionContext context);
    void onAfterTurn(int turn, ToolExecutionResult result);
}

// 使用策略
engine.setStrategy(new MyCustomStrategy());
```

## 6. 错误处理

### 6.1 工具执行失败

```java
try {
    result = executeTool(...);
} catch (Exception e) {
    return ToolExecutionResult.failed(toolName, e.getMessage());
}
```

### 6.2 LLM 调用失败

```java
try {
    llmResponse = callLLM(...);
} catch (Exception e) {
    // 回退到规则匹配
    return handleRuleBasedChat(...);
}
```

### 6.3 超时处理

```java
if (turn >= MAX_TURNS) {
    return ToolExecutionResult.failed("timeout", "任务执行超时");
}
```

## 7. 性能优化

### 7.1 并发执行

对于独立的任务，可以并发执行：
```java
if (plansToDelete.size() > 1) {
    // 并发删除
    plans.parallelStream().forEach(plan -> {
        planService.deletePlan(plan.getId());
    });
}
```

### 7.2 缓存

```java
@Cacheable("plans")
public List<PlanVO> getPlanList(Long userId) {
    return planMapper.findByUserId(userId);
}
```

## 8. 测试用例

### 8.1 单元测试

```java
@Test
public void testDeleteAllPlans() {
    // 准备数据
    when(planService.getPlanList(1L)).thenReturn(plans);
    
    // 执行
    ToolExecutionResult result = planTools.handleDelete(...);
    
    // 断言
    assertTrue(result.isNeedContinue());
    assertEquals(2, result.getData().get("plans_to_delete"));
}
```

### 8.2 集成测试

```java
@Test
public void testMultiTurnExecution() {
    Map<String, Object> response = engine.execute(
        "user123",
        "session456",
        "删除我所有的用药计划",
        false,
        false
    );
    
    assertEquals(true, response.get("success"));
    assertTrue(((List) response.get("tools_called")).size() >= 2);
}
```

## 9. 迁移计划

### 阶段 1：基础设施（已完成）
- [x] 创建 ExecutionStatus 枚举
- [x] 创建 ToolExecutionResult 类
- [x] 创建 MultiTurnExecutionEngine 框架

### 阶段 2：工具改造
- [ ] 修改 PlanTools 返回 ToolExecutionResult
- [ ] 修改 RagTools 返回 ToolExecutionResult
- [ ] 修改 WebSearchTools 返回 ToolExecutionResult

### 阶段 3：集成测试
- [ ] 测试删除所有计划
- [ ] 测试批量创建计划
- [ ] 测试复杂查询场景

### 阶段 4：生产部署
- [ ] 性能测试
- [ ] 灰度发布
- [ ] 监控告警

## 10. 总结

多轮 Function Call 引擎的核心优势：

1. **解耦**：工具执行与 LLM 调用分离
2. **可扩展**：新增工具无需修改引擎
3. **灵活**：支持自定义执行策略
4. **健壮**：完善的错误处理机制
5. **可观测**：完整的执行追踪

通过引入多轮执行引擎，我们可以优雅地处理需要多步操作的任务，同时保持代码的可维护性和扩展性。
