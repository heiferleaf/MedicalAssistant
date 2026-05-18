# 模块六 Agent 架构优化计划

> 基于模块五压测结果（同步 Chat 平均 6748ms，SSE 平均 14710ms 且存在错误率）制定的优化方案。

---

## 一、缓存体系（当前状态：❌ 未生效）

### 问题

`AiCacheManager` 类已实现完整的 Redis 缓存存取方法，但**没有任何 Service 注入使用它**，完全是一段死代码。

| 缓存类型 | 是否定义 | 是否使用 | TTL |
|---------|---------|---------|-----|
| OCR 结果缓存 | ✅ `cacheOcrResult()` / `getCachedOcrResult()` | ❌ 未被调用 | 3600s |
| RAG 结果缓存 | ✅ `cacheRagResult()` / `getCachedRagResult()` | ❌ 未被调用 | 1800s |
| Predict 结果缓存 | ✅ `cachePredictResult()` / `getCachedPredictResult()` | ❌ 未被调用 | 600s |
| 热点 Chat 缓存 | ⚠️ 只有前缀常量 `ai:cache:chat:` | ❌ 无存取方法 | 未定义 |
| 工具调用缓存 | ❌ 未设计 | ❌ | - |

### 待办

- [ ] 在 `OcrService` 中注入 `AiCacheManager`，OCR 请求前先查缓存
- [ ] 在 `RagService` 中注入 `AiCacheManager`，RAG 请求前先查缓存
- [ ] 在 `PredictService` 中注入 `AiCacheManager`，Predict 请求前先查缓存
- [ ] 实现 Chat 结果缓存：相同问题（MD5）在 TTL 内直接返回缓存
- [ ] 实现工具调用缓存：相同参数的工具调用结果缓存复用

---

## 二、异步架构（当前状态：✅ 基础完整）

### 现状

| 组件 | 状态 | 说明 |
|------|------|------|
| 请求分级 L1-L4 | ✅ | 按消息长度/内容自动分级 |
| aiExecutor 线程池 | ✅ | core=8, max=20, queue=100 |
| CompletableFuture 异步执行 | ✅ | 非阻塞提交 |
|  per-grade timeout | ✅ | L1=2s, L2=10s, L3=30s, L4=120s |
| 客户端轮询获取结果 | ✅ | `GET /api/agent/task/{taskId}` |
| **任务完成推送通知** | **❌** | **客户端只能轮询** |

### 待办

- [ ] 任务完成后通过 WebSocket 推送结果给客户端（减少轮询开销）
- [ ] 可选：增加 Webhook 回调注册机制

---

## 三、容错与弹性（当前状态：⚠️ 只保护了 Flask）

### 问题

Resilience4j 的 `@RateLimiter`、`@CircuitBreaker`、`@Retry` 只加在了 `UnifiedFlaskClient` 的三个方法上（OCR/RAG/Predict），**LLM Chat 调用完全没有保护**。

application.yaml 中定义的限流实例：
```yaml
llmChat:   limit-for-period: 20   # ← 定义了但没有 @RateLimiter 引用
agentChat: limit-for-period: 10   # ← 定义了但没有 @RateLimiter 引用
```

同时 SSE 流式输出缺少客户端重连机制。

### 待办

- [ ] 给 `LangChain4jConfig` 的 LLM 调用添加 `@RateLimiter(name = "llmChat")` 保护
- [ ] 给同步 Chat 入口添加 `@RateLimiter(name = "agentChat")` 保护
- [ ] LLM 调用添加 `@CircuitBreaker(name = "llmChat")`，失败时快速降级
- [ ] SSE 客户端实现自动重连机制（指数退避）
- [ ] SSE 服务端实现断点续传（记录已发送的位置）

---

## 四、同步 Chat 性能优化（当前平均 6748ms）

### 问题分析

主要耗时在 LLM 远程调用。当前超长 system prompt（~200 行）增加了每次调用的 token 消耗。

### 待办

- [ ] 精简 system prompt，移除冗余示例和重复指令
- [ ] 实现 Chat 缓存：完全相同的问法直接返回历史结果
- [ ] 考虑使用更快的模型（当前 `qwen-turbo`，可测试 `qwen-plus` 或 `qwen-max` 的差异）

---

## 五、SSE 流式优化（当前平均 14710ms，有错误率）

### 问题分析

当前 SSE 存在两个结构性问题：

1. **模拟流式**：`chatStream()` 等待 LLM 完整返回后，每 50ms 发 10 个字符模拟流式效果，不是真正的 streaming
2. **无保护**：LLM 调用没有熔断，DashScope 超时限流时直接抛异常

### 待办

- [ ] 对接 DashScope 真正的 streaming API（`QwenStreamingChatModel` 或 `ChatModel.stream()`），实现逐 token 推送
- [ ] 添加 LLM 级别的 RateLimiter 和 CircuitBreaker
- [ ] 客户端 SSE 重连逻辑（EventSource 或 Fetch API）
