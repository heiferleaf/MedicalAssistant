# 多轮 Function Call 引擎实现总结

## 1. 问题解决

### 原有问题
❌ **单次 Function Call 限制**：LLM 一次只能调用一个工具，无法完成多步任务  
❌ **耦合严重**：工具执行逻辑与 LLM 调用逻辑混在一起  
❌ **扩展性差**：新增工具需要修改多处代码  

**典型案例**：
```
用户："删除我所有的用药计划"

当前行为：
1. LLM 调用 spring_plan_query → 返回 2 个计划
2. ❌ 期望继续调用 spring_plan_delete × 2 次，但实际没有

期望行为：
1. LLM 调用 spring_plan_query → 返回 2 个计划
2. ✅ 引擎自动继续调用 spring_plan_delete(planId=1)
3. ✅ 引擎自动继续调用 spring_plan_delete(planId=2)
4. ✅ 返回最终结果
```

### 解决方案
✅ **多轮执行引擎**：自动处理多轮 Function Call  
✅ **低耦合架构**：工具与引擎分离  
✅ **高扩展性**：新增工具无需修改引擎  

## 2. 架构设计

### 2.1 核心组件

```
┌─────────────────────────────────────────┐
│   SimpleMultiTurnEngine                 │
│   (多轮执行引擎)                         │
├─────────────────────────────────────────┤
│  - execute()                            │
│  - 循环调用 LLM (max 5 轮)               │
│  - 执行工具                             │
│  - 判断是否继续                         │
└─────────────────────────────────────────┘
           │
           ├──► LLM Service (阿里云 Qwen)
           │       - chatWithFunction()
           │
           └──► Tool Handlers
                   - PlanTools
                   - RagTools
                   - WebSearchTools
```

### 2.2 执行流程

```
用户请求
   │
   ▼
┌─────────────────────────────────┐
│  SimpleMultiTurnEngine.execute()│
│  ┌───────────────────────────┐  │
│  │ for turn in 0..MAX_TURNS: │  │
│  │   1. 调用 LLM             │  │
│  │   2. 执行工具             │  │
│  │   3. 判断 need_continue   │  │
│  │      - true → 继续        │  │
│  │      - false → 返回       │  │
│  └───────────────────────────┘  │
└─────────────────────────────────┘
   │
   ▼
最终响应
```

## 3. 实现细节

### 3.1 新增文件

1. **ExecutionStatus.java** - 执行状态枚举
   - `SUCCESS` - 成功
   - `CONTINUE` - 需要继续
   - `FAILED` - 失败
   - `NEED_CONFIRM` - 需要确认

2. **ToolExecutionResult.java** - 工具执行结果封装
   - 状态、消息、数据
   - `needContinue` 标志
   - `nextStepSuggestion` 建议

3. **SimpleMultiTurnEngine.java** - 多轮执行引擎（核心）
   - `execute()` - 主入口
   - `callLLM()` - 调用 LLM
   - `executeTool()` - 执行工具
   - `buildFinalResponse()` - 构建响应

4. **MultiTurnExecutionEngine.java** - 完整版引擎（未来使用）
   - 更完善的错误处理
   - 支持自定义策略
   - 支持并发执行

### 3.2 修改文件

1. **AgentOrchestratorService.java**
   ```java
   // 新增依赖
   @Autowired
   private SimpleMultiTurnEngine multiTurnEngine;
   
   // 修改 handleLlmChat
   private Map<String, Object> handleLlmChat(...) {
       // 使用多轮引擎
       return multiTurnEngine.execute(...);
   }
   
   // 保留单轮执行作为回退
   private Map<String, Object> handleSingleTurnLlmChat(...) {
       // 原有逻辑
   }
   ```

2. **PlanTools.java**
   ```java
   // 修改 handlePlanDelete
   private Map<String, Object> handlePlanDelete(...) {
       // 支持 deleteAll 参数
       if (deleteAll) {
           List<PlanVO> plans = planService.getPlanList(uid);
           for (PlanVO plan : plans) {
               planService.deletePlan(uid, plan.getId());
           }
       }
       // ...
       
       // 返回 need_continue 标志
       result.put("need_continue", false);
   }
   ```

3. **AgentPromptTemplate.java**
   ```java
   // 更新系统提示
   "7. 如果用户要删除某个计划：
      - 如果用户明确说了计划 ID，直接调用 spring.plan.delete
      - 如果用户只说了药品名，直接调用 spring.plan.delete 并传入 medicineName
      - 如果用户说'删除所有计划'，先调用 spring.plan.query 查询，
        然后对每个计划调用 spring.plan.delete"
   ```

## 4. 使用示例

### 4.1 删除所有计划

**请求：**
```json
POST /api/agent/chat
{
  "user_id": "1",
  "session_id": "test-123",
  "message": "删除我所有的用药计划",
  "with_trace": true
}
```

**执行流程：**
```
第 1 轮：
  LLM → spring_plan_query
  结果：[{id:1, name:"阿司匹林"}, {id:2, name:"布洛芬"}]
  
第 2 轮：
  LLM → spring_plan_delete(planId=1)
  结果：成功删除
  
第 3 轮：
  LLM → spring_plan_delete(planId=2)
  结果：成功删除
  
返回：
{
  "success": true,
  "assistant_message": "已成功删除 2 个用药计划",
  "deleted_count": 2,
  "trace": {
    "tools_called": ["spring_plan_query", "spring_plan_delete", "spring_plan_delete"],
    "execution_turns": 3
  }
}
```

### 4.2 删除特定药品

**请求：**
```json
{
  "message": "删除阿司匹林用药计划"
}
```

**执行流程：**
```
第 1 轮：
  LLM → spring_plan_delete(medicineName="阿司匹林")
  PlanTools 查找匹配的计划
  删除所有匹配的计划
  
返回：
{
  "success": true,
  "assistant_message": "已成功删除 1 个与\"阿司匹林\"相关的用药计划",
  "deleted_count": 1
}
```

## 5. 扩展性设计

### 5.1 新增工具（零侵入）

**步骤 1：在 PlanTools 中添加方法**
```java
public Map<String, Object> handleBatchCreate(...) {
    // 实现
}
```

**步骤 2：添加工具定义**
```java
public ObjectNode createPlanBatchCreateTool() {
    // 定义工具参数
}
```

**步骤 3：注册到工具列表**
```java
public ArrayNode getAllPlanTools() {
    tools.add(createPlanBatchCreateTool());
}
```

**无需修改引擎代码！**

### 5.2 自定义执行策略

```java
public interface ExecutionStrategy {
    boolean shouldContinue(ToolExecutionResult result, int turn);
    void onBeforeTurn(int turn, ExecutionContext context);
    void onAfterTurn(int turn, ToolExecutionResult result);
}

// 使用
engine.setStrategy(new MyCustomStrategy());
```

## 6. 性能优化

### 6.1 并发执行
```java
// 并发删除多个计划
plans.parallelStream().forEach(plan -> {
    planService.deletePlan(uid, plan.getId());
});
```

### 6.2 缓存优化
```java
@Cacheable("plans")
public List<PlanVO> getPlanList(Long userId) {
    return planMapper.findByUserId(userId);
}
```

### 6.3 批量操作
```java
@Transactional
public void deletePlans(Long userId, List<Long> planIds) {
    planMapper.batchDelete(userId, planIds);
}
```

## 7. 错误处理

### 7.1 工具执行失败
```java
try {
    result = executeTool(...);
} catch (Exception e) {
    return ToolExecutionResult.failed(toolName, e.getMessage());
}
```

### 7.2 LLM 调用失败
```java
try {
    llmResponse = callLLM(...);
} catch (Exception e) {
    // 回退到单轮执行
    return handleSingleTurnLlmChat(...);
}
```

### 7.3 超时处理
```java
if (turn >= MAX_TURNS) {
    return errorResponse("任务执行超时");
}
```

## 8. 测试验证

### 8.1 单元测试
```java
@Test
public void testDeleteAllPlans() {
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

### 8.2 Postman 测试
1. 导入测试集合
2. 设置环境变量
3. 运行测试场景

## 9. 监控指标

### 9.1 关键指标
- 执行次数
- 错误次数
- 平均执行时间
- 最大执行轮数

### 9.2 日志记录
```bash
# 查看多轮执行日志
tail -f logs/application.log | grep "多轮执行引擎"

# 查看工具调用日志
tail -f logs/application.log | grep "调用工具"
```

## 10. 最佳实践

### 10.1 工具设计原则
✅ 单一职责  
✅ 幂等性  
✅ 错误处理  
✅ 日志记录  

### 10.2 多轮执行策略
✅ 限制轮数（3-5 轮）  
✅ 明确终止条件  
✅ 上下文管理  
✅ 回退机制  

### 10.3 用户体验
✅ 进度提示  
✅ 错误恢复  
✅ 结果确认  
✅ 性能感知  

## 11. 文件清单

### 新增文件
- `ExecutionStatus.java` - 执行状态枚举
- `ToolExecutionResult.java` - 执行结果封装
- `SimpleMultiTurnEngine.java` - 多轮执行引擎（核心）
- `MultiTurnExecutionEngine.java` - 完整版引擎
- `MULTI_TURN_ENGINE_DESIGN.md` - 设计文档
- `MULTI_TURN_USAGE_GUIDE.md` - 使用指南

### 修改文件
- `AgentOrchestratorService.java` - 集成多轮引擎
- `PlanTools.java` - 支持多轮执行
- `AgentPromptTemplate.java` - 更新系统提示

## 12. 总结

### 核心优势
✅ **自动化多步操作** - 无需用户手动触发多次  
✅ **解耦设计** - 工具与引擎分离  
✅ **高扩展性** - 新增工具零侵入  
✅ **健壮性** - 完善的错误处理  
✅ **可观测** - 完整的执行追踪  

### 未来规划
- [ ] 支持并发执行工具
- [ ] 添加工具执行缓存
- [ ] 实现自定义执行策略
- [ ] 集成监控系统
- [ ] 性能优化（批量操作、连接池等）

### 技术亮点
1. **责任链模式** - 多轮执行形成处理链
2. **策略模式** - 支持不同的执行策略
3. **门面模式** - 封装复杂的 LLM 交互
4. **开闭原则** - 对扩展开放，对修改关闭
5. **依赖倒置** - 依赖抽象，不依赖具体实现

---

**多轮 Function Call 引擎** 为 Agent 系统提供了强大的自动化能力，是构建智能助手的核心基础设施！🚀
