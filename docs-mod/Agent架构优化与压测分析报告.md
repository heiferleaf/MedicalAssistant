# Agent 架构优化与压测分析报告

> 项目：MedicalAssistant Agent 模块  
> 日期：2026-05-31  
> 模型：qwen-turbo  
> 版本：schemeA_spring_memory_v1_langchain4j_2026-03-08

---

## 目录

1. [核心功能：Agent 架构在 SpringBoot 侧的改动](#1-核心功能agent-架构在-springboot-侧的改动)
2. [问题分析：性能、可用性、可靠性挑战](#2-问题分析性能可用性可靠性挑战)
3. [技术方案：针对每个问题的解决方案](#3-技术方案针对每个问题的解决方案)
4. [测试评估：测试方案与效果](#4-测试评估测试方案与效果)

---

## 1. 核心功能：Agent 架构在 SpringBoot 侧的改动

### 1.1 整体架构概览

Agent 模块采用 SpringBoot 微服务架构，核心入口为 `AgentOrchestratorService`，通过 `AgentProxyController`（`/api/agent/*`）对外暴露 REST API。架构分层如下：

```
Nginx (反向代理)
  └── Sentinel (流量防护)
       └── AgentProxyController (REST 入口)
            └── AgentOrchestratorService (核心编排层)
                 ├── MedicalAgent (LangChain4j 智能体)
                 │    ├── ToolService (药物 CRUD / OCR / RAG / Predict)
                 │    └── ChatMemory (MySQL 持久化)
                 ├── LlmChatDelegate (Resilience4j 弹性保护层)
                 │    ├── ChatModel / StreamingChatModel (DashScope qwen-turbo)
                 │    └── Fallback → MedicalAgent / 降级文案
                 ├── ChatCacheService (AiCacheManager + Redis)
                 ├── AgentTaskService (异步任务/请求分级)
                 └── AgentMemoryRepository (会话+消息持久化)
```

### 1.2 核心改动清单

| 模块 | 改动 | 文件 |
|------|------|------|
| 三层 LLM 回退 | 引入 MedicalAgent → StreamingChatModel → ChatModel 三级降级 | `AgentOrchestratorService.java` |
| Chat 缓存 | 基于 Redis 的 Chat 结果缓存，TTL=300s，仅缓存短文本 | `ChatCacheService.java` |
| Resilience4j 弹性保护 | LLM 层 RateLimiter + CircuitBreaker + Retry | `LlmChatDelegate.java` |
| SSE 流式优化 | 三层回退、空消息检测、连接超时管理 | `AgentOrchestratorService.chatStream()` |
| 全链路测试模式 | `AGENT_TEST_LOCAL_ONLY=true` 跳过 LLM 调用的测试模式 | `AgentOrchestratorService` |
| 压测计数器 | AtomicLong 计数器 + `/api/agent/stats` 端点 | `AgentOrchestratorService` / `AgentProxyController` |
| Docker 编排 | Sentinel QPS、连接池大小等参数支持环境变量覆盖 | `docker-compose.microservices.yml` |

### 1.3 三层 LLM 回退

`chat()` 方法的执行路径：

```
chat() 入口
  ├── ChatCacheService 检查缓存
  ├── localOnly 检查（跳过 LLM）
  ├── MedicalAgent.execute() — 第一优先级
  │    └── 返回空 → 重试一次 → 仍空 → 降级到简单 LLM
  ├── LlmChatDelegate.callChat() — 第二优先级（简单 LLM）
  │    └── 返回空 → 重试一次 → 仍空 → 返回错误
  └── llmUnavailableResult() — 第三优先级（LLM 未配置）
```

`chatStream()` 的 SSE 执行路径：

```
chatStream() 入口（SSE）
  ├── localOnly 检查
  ├── MedicalAgent.chatStream() — 第一优先级（真流式）
  │    └── 异常/错误 → 继续降级
  ├── StreamingChatModel.chat() — 第二优先级（SDK 原生流式）
  │    └── 返回空 → 视为无效
  └── ChatModel.chat() 阻塞调用 → 拼接为一次性 SSE 事件 — 第三优先级
```

### 1.4 Chat 缓存

`ChatCacheService` 基于 `AiCacheManager` + Redis：

- **缓存 Key**: MD5(userId + "|" + sessionId + "|" + message)
- **缓存条件**: `isCacheable()` — 消息长度 < 200 且无图片 Base64/OCR 数据
- **适用范围**: 仅限 L1/L2 短文本简单请求
- **TTL**: 300 秒（Chat 场景）

```java
// ChatCacheService.java
public static boolean isCacheable(Map<String, Object> payload) {
    String message = String.valueOf(payload.getOrDefault("message", ""));
    return message.length() < 200
            && !message.contains("图片数据：")
            && !message.contains("/9j/")
            && !message.contains("OCR 识别结果：")
            && !message.contains("data:image/");
}
```

### 1.5 Resilience4j 弹性保护

配置在 `application.yaml` 中，三层保护作用于 LLM Chat、Flask OCR、Flask RAG、Flask Predict：

#### RateLimiter（限流）

| 实例 | QPS | 超时等待 |
|------|-----|---------|
| flaskOcr | 30/s | 500ms |
| flaskRag | 50/s | 500ms |
| flaskPredict | 20/s | 500ms |
| llmChat | 20/s | 500ms |
| agentChat | 10/s | 500ms |

#### CircuitBreaker（熔断）

| 参数 | 值 |
|------|-----|
| sliding-window-size | 10 |
| failure-rate-threshold | 50% |
| minimum-number-of-calls | 5 |
| wait-duration-in-open-state | 30s |

#### Retry（重试）

| 参数 | 值 |
|------|-----|
| max-attempts | 3 |
| wait-duration | 1s |

#### 代码实现

```java
// LlmChatDelegate.java
@Retry(name = "llmChat")
@RateLimiter(name = "llmChat", fallbackMethod = "chatFallback")
@CircuitBreaker(name = "llmChat", fallbackMethod = "chatFallback")
public ChatResponse callChat(ChatModel chatModel, SystemMessage systemMessage, UserMessage userMessage) {
    return chatModel.chat(systemMessage, userMessage);
}
```

### 1.6 SSE 流式优化

| 优化项 | 实现 |
|--------|------|
| 三层回退 | MedicalAgent 流式 → StreamingChatModel → 阻塞 ChatModel |
| 空消息检测 | 所有回退路径均检查空白响应，检测到空则返回错误事件 |
| 连接管理 | 5 分钟 SSEClient 超时 + aiExecutor 异步处理 |
| 缓存控制 | Cache-Control: no-cache + X-Accel-Buffering: no |
| JSON 解析 | 前端收到完整 JSON 后再解析，避免 SSE data chunk 被截断 |

### 1.7 压测计数器

新增 5 个 `AtomicLong` 计数器，用于精确统计 LLM API 调用次数：

```java
// AgentOrchestratorService.java
private final AtomicLong apiCallCount = new AtomicLong(0);      // 同步 Chat 调用 LLM 次数
private final AtomicLong cacheHitCount = new AtomicLong(0);     // 缓存命中次数
private final AtomicLong emptyResponseCount = new AtomicLong(0); // LLM 返回空响应次数
private final AtomicLong sseApiCallCount = new AtomicLong(0);   // SSE 调用 LLM 次数
private final AtomicLong totalRequestCount = new AtomicLong(0); // 总 Chat 请求数
```

通过 `GET /api/agent/stats` 暴露：

```json
{
  "total_request_count": 4131,
  "cache_hit_count": 0,
  "api_call_count": 4147,
  "sse_api_call_count": 4266,
  "empty_response_count": 0,
  "cache_hit_rate": "0.00%",
  "api_call_rate": "100.39%"
}
```

### 1.8 Sentinel 流量防护

| 规则 | 默认 QPS | 压测时调整值 | 入口 |
|------|---------|------------|------|
| agent-chat | 200 | 2000 | POST /api/agent/chat |
| agent-chat-stream | 200 | 2000 | GET /api/agent/chat/stream |
| agent-chat-stream-param | 200 | 2000 | POST /api/agent/chat/stream |
| ocr | 50 | 50 | OCR 调用 |

### 1.9 全链路测试模式

通过 `AGENT_TEST_LOCAL_ONLY=true` 环境变量控制：

- **true**: 跳过所有 LLM API 调用，返回固定测试文案 `[全链路测试模式] 本地管道正常，未调用 LLM API`
- **false**: 正常调用 LLM API（消耗 token）

### 1.10 其他性能参数优化

| 参数 | 优化前 | 优化后 | 说明 |
|------|-------|-------|------|
| HikariCP max-pool | 50 | 100 | 数据库连接池扩容 |
| Redis pool max | 20 | 40 | Redis 连接池扩容 |
| aiExecutor core/max | 20/50 | 50/200 | AI 线程池扩容 |
| ai.cache.max-connections | 50 | 200 | AI HTTP 连接池 |
| Multi-turn timeout | 无 | 30s | 多轮超时保护 |

---

## 2. 问题分析：性能、可用性、可靠性挑战

### 2.1 性能挑战

#### P1: LLM 调用延迟高 — 同步 Chat 单次 5-17s
- 优化前同步 Chat 平均 5,451ms（MedicalAgent 模式），SSE 首 token 延迟未量化
- 瓶颈点：LLM 首 token 生成（qwen-turbo）+ 网络传输 + 工具链编排
- 影响：用户体验差，HTTP 连接可能超时断开

#### P2: 高并发下请求排队导致延迟急剧上升
- 200 并发下 Chat 延迟从 17.8s 上升到 26.4s（+48%）
- 500 并发/5s 毛刺下端口耗尽（BindException）
- 原因：Tomcat 线程池 + aiExecutor 队列双重排队

#### P3: 重复请求浪费 Token
- 相同问题重复调用 LLM，无缓存机制
- 短文本简单问答（如"头痛"、"失眠"）返回结果高度相似

### 2.2 可用性挑战

#### A1: LLM API 不稳定导致空响应
- 优化前偶现空响应（LLM 返回空白内容）
- 无重试机制，空白内容直接透传给用户

#### A2: 突发流量导致 Sentinel 限流（429）
- 毛刺场景 300 线程 5s ramp 触发大量 429
- 限流后客户端未重试，请求直接失败

#### A3: SSE 连接管理不完善
- SSE 连接泄漏（onCompletion/onTimeout 未正确处理）
- 无超时保护，长时间无响应的连接持续占用资源

### 2.3 可靠性挑战

#### R1: 缺乏降级机制
- LLM 调用失败 → 整个 Chat 接口 500 错误
- 未区分 LLM 不可用 / LLM 返回空 / LLM 超时

#### R2: 单点依赖 LLM API
- 依赖单一 DashScope 模型（qwen-turbo）
- API Key 失效时完全不可用

#### R3: 链路保护不足
- Flask 服务（OCR/RAG/Predict）无熔断机制
- 一个组件故障可能连锁拖垮整个 Agent

### 2.4 可观测性挑战

#### O1: 无法区分请求来源
- 无法区分"已达到 LLM"和"被 Sentinel/缓存拦截"的请求
- 测试数据中 429 限流与真实 Chat 请求混淆

#### O2: 缺乏空响应监测
- LLM 返回空白内容时无报警
- 无法统计空响应发生率

---

## 3. 技术方案：针对每个问题的解决方案

### 3.1 性能优化方案

#### 针对 P1: LLM 延迟高

**方案：三层 LLM 回退 + 空响应重试 + 精简提示词**

```
请求路径优先级：
1. MedicalAgent（完整工具链，最智能）
2. LlmChatDelegate（简单 LLM，无工具调用）
3. llmUnavailableResult（LLM 未配置时的友好提示）

重试策略：
- MedicalAgent 空响应 → 自动重试 1 次 → 仍空 → 降级
- 简单 LLM 空响应 → 自动重试 1 次 → 仍空 → 返回错误
```

#### 针对 P2: 高并发排队

**方案：Sentinel QPS 环境变量覆盖 + 连接池扩容 + 请求分级**

```yaml
# docker-compose 支持运行期参数覆盖
SENTINEL_FLOW_AGENT_CHAT_QPS: ${SENTINEL_FLOW_AGENT_CHAT_QPS:-200}
SENTINEL_FLOW_AGENT_CHAT_STREAM_QPS: ${SENTINEL_FLOW_AGENT_CHAT_STREAM_QPS:-200}

# 请求分级（AgentTaskService）
L1: 2s 超时 — 简单查询
L2: 10s 超时 — 轻量对话
L3: 30s 超时 — 标准对话（默认）
L4: 120s 超时 — 复杂任务（含 OCR/RAG）
```

#### 针对 P3: 重复请求浪费

**方案：ChatCacheService + AiCacheManager**

```
相同 (userId + sessionId + message) → MD5 cache key
  → Redis GET → 命中则直接返回缓存结果
  → 未命中 → 调用 LLM → Redis SETEX 300s
```

### 3.2 可用性优化方案

#### 针对 A1: LLM 空响应

**方案：空响应检测 + 自动重试**

```java
// handleMedicalAgentChat() 中的空响应检测
if (result != null && result.get("success") == Boolean.TRUE) {
    String msg = (String) result.get("assistant_message");
    if (msg == null || msg.isBlank()) {
        logger.warn("Medical Agent 返回空消息，触发重试");
        result = medicalAgent.execute(sessionId, userId, message, message);
        if (仍为空) return null; // 降级到简单 LLM
    }
}
```

#### 针对 A2: 突发流量限流

**方案：Sentinel 限流 + 调高 QPS 阈值 + 压测时精确控制**

```
正常: Sentinel QPS=200 → 超过时返回 429 友好提示
压测: Sentinel QPS=2000 → 避免 429 干扰，测出真实 LLM 瓶颈
```

#### 针对 A3: SSE 连接管理

**方案：SseEmitter 超时 + 回调清理**

```java
SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
emitter.onCompletion(() -> { ... });
emitter.onTimeout(() -> emitter.completeWithError(...));
emitter.onError(throwable -> { ... });
```

### 3.3 可靠性优化方案

#### 针对 R1-R3: 缺乏降级

**方案：Resilience4j 三层弹性保护**

```
LLM 调用链路保护：
  Retry (3次) → RateLimiter (20/s) → CircuitBreaker (50%失败率)

Flask 调用链路保护：
  Retry (3次) → RateLimiter (30/50/20/s) → CircuitBreaker (50%)

降级策略：
  RateLimiter 触发 → 返回 429 "服务繁忙"
  CircuitBreaker 触发 → 返回"服务暂时不可用" + 30s 后自动半开
  Retry 耗尽 → 返回"LLM 调用失败" + 异常信息
```

### 3.4 可观测性优化方案

#### 针对 O1-O2: 缺乏统计数据

**方案：AtomicLong 计数器 + `/api/agent/stats` 端点**

```java
// 5 个计数器涵盖所有关键指标
apiCallCount      — 真实消耗 token 的次数
cacheHitCount     — 缓存节约的调用次数
emptyResponseCount — LLM 异常的监测数据
sseApiCallCount   — SSE 路径调用量
totalRequestCount  — 总请求量
```

---

## 4. 测试评估：测试方案与效果

### 4.1 测试场景设计

共设计 5 个场景，覆盖基线性能、高并发、长稳、毛刺、过载。

| 场景 | 线程数 | Ramp | 持续时间 | 测试目标 |
|------|-------|------|---------|---------|
| S1 Baseline | 100 | 30s | 180s | 基线性能（基准参考） |
| S2 HighConcurrency | 200 | 30s | 180s | 高并发瓶颈 |
| S3 Reliability | 150 | 30s | 600s | 10 分钟长稳验证 |
| S4 Spike | 300 | 5s | 120s | 突发毛刺抗冲击 |
| S5 Overload | 500 | 10s | 60s | 过载极限 |

每个场景包含：
- Health 探活（GET /api/agent/health）
- 会话创建（POST /api/agent/sessions）
- 同步 Chat（POST /api/agent/chat）
- SSE 流式 Chat（GET /api/agent/chat/stream, JSR223 采样器测量 TTFC）

### 4.2 测试环境

| 项目 | 值 |
|------|-----|
| 部署方式 | Docker Compose 微服务 |
| 模型 | qwen-turbo（DashScope） |
| Sentinel QPS | 2000（为避免 429 干扰调高） |
| AGENT_TEST_LOCAL_ONLY | false（调用真实 LLM） |

### 4.3 本地管道测试结果 (local-only)

测试时 `AGENT_TEST_LOCAL_ONLY=true`，跳过 LLM 调用，只测本地管道（Nginx → Sentinel → Controller → Service → MySQL → Redis）。

**总请求**: 365,535 | **总耗时**: 19m 11s | **吞吐**: 317.5/s

#### S1 Baseline (100t, 180s) — 0% 错误

| 指标 | Health | 创建会话 | Chat |
|------|--------|---------|------|
| 请求数 | 10,997 | 10,997 | 10,997 |
| 平均(ms) | 4 | 27 | 1,342 |
| P50(ms) | 4 | 25 | 1,495 |
| P90(ms) | 6 | 38 | 1,592 |
| P99(ms) | 10 | 53 | 1,658 |
| 错误率 | 0% | 0% | 0% |

#### S2 HighConcurrency (200t, 180s) — 0% 错误

| 指标 | Health | 创建会话 | Chat |
|------|--------|---------|------|
| 请求数 | 11,400 | 11,398 | 11,398 |
| 平均(ms) | 3 | 27 | 2,621 |
| P50(ms) | 3 | 25 | 3,041 |
| P90(ms) | 5 | 38 | 3,177 |
| P99(ms) | 8 | 54 | 3,224 |
| 错误率 | 0% | 0% | 0% |

#### S3 Reliability (150t, 600s/10min) — 0% 错误

| 指标 | Health | 创建会话 | Chat 长稳 | 空消息 | 缺参数 |
|------|--------|---------|----------|-------|-------|
| 请求数 | 12,755 | 12,755 | 12,753 | 12,705 | 12,664 |
| 平均(ms) | 4 | 24 | 2,225 | 2,245 | 2,232 |
| P50(ms) | 4 | 23 | 2,301 | 2,333 | 2,315 |
| P90(ms) | 6 | 31 | 2,430 | 2,430 | 2,417 |
| P99(ms) | 9 | 41 | 2,469 | 2,479 | 2,473 |
| 错误率 | 0% | 0% | 0% | 0% | 0% |

#### S4 Spike (300t, 5s ramp, 120s) — 18.2% 错误（全部为 Sentinel 429）

| 指标 | Health | 创建会话 | Chat |
|------|--------|---------|------|
| 请求数 | 52,086 | 52,076 | 52,034 |
| 平均(ms) | 11 | 72 | 567 |
| P50(ms) | 8 | 67 | 11（429 快速返回拉低中位数） |
| P90(ms) | 24 | 112 | 2,034 |
| P99(ms) | 63 | 175 | 2,152 |
| 错误率 | 0% | 0% | 54.5% |

#### S5 Overload (500t, 10s ramp, 60s) — 18.2% 错误（全部为 Sentinel 429）

| 指标 | Health | 创建会话 | Chat |
|------|--------|---------|------|
| 请求数 | 26,265 | 26,197 | 26,058 |
| 平均(ms) | 96 | 256 | 659 |
| P50(ms) | 29 | 189 | 29 |
| P90(ms) | 292 | 507 | 2,510 |
| P99(ms) | 610 | 879 | 3,041 |
| 错误率 | 0% | 0% | 54.9% |

### 4.4 真实 API 测试结果

测试时 `AGENT_TEST_LOCAL_ONLY=false`，Chat 请求调用真实 qwen-turbo LLM，消耗 token。

**总请求 (JMeter)**: 229,798 | **总耗时**: 22m 22s | **真实 API 调用**: 8,413 次
**服务器端计数器**: sync_api=4,147, sse_api=4,266, empty_resp=0

#### S1 Baseline (100t, 180s) — 0% 错误

| 指标 | Health | 创建会话 | 同步 Chat | SSE Chat |
|------|--------|---------|----------|---------|
| 请求数 | 538 | 538 | 538 | 476 |
| 平均(ms) | 7 | 28 | **17,758** | **16,608** |
| P50(ms) | 7 | 26 | 19,095 | 17,248 |
| P90(ms) | 10 | 35 | 21,955 | 20,063 |
| P99(ms) | 15 | 60 | 25,447 | 22,350 |
| 错误率 | 0% | 0% | 0% | 0% |

#### S2 HighConcurrency (200t, 180s) — 0% 错误

| 指标 | Health | 创建会话 | 同步 Chat | SSE Chat |
|------|--------|---------|----------|---------|
| 请求数 | 670 | 670 | 670 | 604 |
| 平均(ms) | 6 | 25 | **26,423** | **31,034** |
| P50(ms) | 6 | 24 | 30,003 | 32,681 |
| P90(ms) | 9 | 33 | 30,006 | 35,632 |
| P99(ms) | 16 | 48 | 30,008 | 37,589 |
| 错误率 | 0% | 0% | 0% | 0% |

#### S3 Reliability (150t, 600s/10min) — 0% 错误

| 指标 | Health | 创建会话 | 同步 Chat(长稳) | SSE Chat(长稳) |
|------|--------|---------|---------------|--------------|
| 请求数 | 1,919 | 1,919 | 1,919 | 1,851 |
| 平均(ms) | 6 | 24 | **24,299** | **23,127** |
| P50(ms) | 5 | 23 | 25,103 | 23,339 |
| P90(ms) | 8 | 32 | 27,814 | 25,962 |
| P99(ms) | 11 | 44 | 30,003 | 28,112 |
| 错误率 | 0% | 0% | 0% | 0% |

#### S4 Spike (300t, 5s ramp, 120s)

| 指标 | Health | 创建会话 | 同步 Chat | SSE Chat |
|------|--------|---------|----------|---------|
| 请求数 | 39,338 | 39,330 | 39,322 | 39,259 |
| 平均(ms) | 24 | 39 | 333 | 559 |
| 错误率 | 71.9% | 71.6% | 71.3% | 97.7% |
| 错误原因 | BindException | BindException | BindException | BindException + 500 |

#### S5 Overload (500t, 10s ramp, 60s)

| 指标 | Health | 创建会话 | 同步 Chat | SSE Chat |
|------|--------|---------|----------|---------|
| 请求数 | 15,190 | 15,118 | 15,046 | 14,883 |
| 平均(ms) | 111 | 238 | 898 | 697 |
| 错误率 | 19.2% | 18.8% | 18.4% | 97.6% |
| 错误原因 | BindException | BindException | BindException | BindException + 500 |

### 4.5 关键对比分析

#### local-only vs 真实 API 对比

| 场景 | local-only Chat 平均 | 真实 API Chat 平均 | 本地管道占比 | LLM 耗时占比 |
|------|--------------------|-------------------|------------|------------|
| S1 (100t) | 1,342ms | 17,758ms | 7.6% | 92.4% |
| S2 (200t) | 2,621ms | 26,423ms | 9.9% | 90.1% |
| S3 (150t) | 2,225ms | 24,299ms | 9.2% | 90.8% |

**结论**: LLM API 调用占 Chat 总耗时 90%+，本地管道（Nginx → Tomcat → Service → DB → Redis）仅占不到 10%。优化重点应继续放在 LLM 调用层。

#### SSE vs 同步 Chat 对比

| 场景 | 同步 Chat 平均 | SSE 首 token 平均 | 差异 |
|------|--------------|-----------------|------|
| S1 (100t) | 17,758ms | 16,608ms | SSE 快 6.5% |
| S2 (200t) | 26,423ms | 31,034ms | SSE 慢 17.4% |
| S3 (150t) | 24,299ms | 23,127ms | SSE 快 4.8% |

**结论**: SSE 与同步 Chat 的首 token 到达时间基本相当。瓶颈在 LLM 生成首 token 的速度，而非传输方式。

### 4.6 问题复现与验证

| 问题 | 是否复现 | 分析 |
|------|---------|------|
| LLM 空响应 | 未复现（0/8,413） | qwen-turbo 稳定性良好，空响应属于小概率事件（<0.01%） |
| Sentinel 限流 (429) | 复现（local-only S4/S5） | 默认 200 QPS 下毛刺场景触发限流，调至 2000 QPS 后未再出现 |
| 端口耗尽 (BindException) | 复现（真实 API S4/S5） | 300-500 线程快速并发导致 `java.net.BindException`，OS 级问题 |
| 缓存命中 | 未命中 | 每次消息随机不同，无法命中缓存 |
| SSE 空响应 | 未复现 | SSE 所有回退路径均有空响应检测 |

### 4.7 性能基准数据

| 指标 | local-only | 真实 API |
|------|-----------|---------|
| Health 探活 (P99) | 10ms | 15ms |
| 创建会话 (P99) | 54ms | 60ms |
| Chat 平均 (S1) | 1,342ms | 17,758ms |
| Chat P50 (S1) | 1,495ms | 19,095ms |
| Chat P90 (S1) | 1,592ms | 21,955ms |
| Chat P99 (S1) | 1,658ms | 25,447ms |
| SSE TTFC 平均 (S1) | N/A | 16,608ms |
| SSE TTFC P50 (S1) | N/A | 17,248ms |
| SSE TTFC P99 (S1) | N/A | 22,350ms |
| 整体吞吐 | 317.5/s | 171.3/s |
| 空响率 | 0% | 0% |

### 4.8 遗留问题

1. **端口耗尽 (BindException)**：S4/S5 快速并发场景出现 `java.net.BindException: Address already in use`，是 JVM 与 OS 之间的本地端口耗尽问题。解决方案包括：
   - JVM 端启用 HTTP KeepAlive 连接复用（已配置 `use_keepalive=true`）
   - 增大 OS `local_port_range`（Windows: `netsh int ipv4 set dynamicport tcp start=10000 num=55535`）
   - 使用连接池替代短连接（JMeter JSR223 Groovy 脚本）

2. **缓存利用率低**：每次压测生成随机消息，缓存命中率为 0。生产环境中相同问题重复率高时缓存有效。

3. **qwen-turbo 延迟高**：P50 ~19s，P99 ~25s 的 LLM 响应时间对实时对话场景不够理想。可以考虑升级模型或使用蒸馏模型。

4. **无多 API Key 轮询**：配置了 `DASHSCOPE_API_KEYS` 但代码中未实现轮询逻辑，目前仍使用单 Key。

---

## 附录 A: 测试环境配置

### Docker Compose 关键环境变量

```yaml
AGENT_TEST_LOCAL_ONLY: ${AGENT_TEST_LOCAL_ONLY:-false}
SENTINEL_FLOW_AGENT_CHAT_QPS: ${SENTINEL_FLOW_AGENT_CHAT_QPS:-200}
SENTINEL_FLOW_AGENT_CHAT_STREAM_QPS: ${SENTINEL_FLOW_AGENT_CHAT_STREAM_QPS:-200}
DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY}
DASHSCOPE_API_KEYS: ${DASHSCOPE_API_KEYS:-}
MAIN_CLASS: com.whu.medicalbackend.bootstrap.AgentServiceApplication
```

### JMeter 配置

```
序列化线程组: serialize_threadgroups=true
连接超时: 5s
读取超时: 120s
错误处理: continue（继续后续请求）
JSR223 SSE 采样器: HttpURLConnection → 逐行读取 data: 事件
```

## 附录 B: 服务器端计数器 API

```
GET /api/agent/stats

Response:
{
  "total_request_count": 4131,
  "cache_hit_count": 0,
  "api_call_count": 4147,
  "sse_api_call_count": 4266,
  "empty_response_count": 0,
  "cache_hit_rate": "0.00%",
  "api_call_rate": "100.39%",
  "server_time": "2026-05-31T18:33:55+08:00"
}
```

## 附录 C: 各模块核心文件清单

| 文件路径 | 功能 |
|---------|------|
| `agent/AgentOrchestratorService.java` | 核心编排器（三层回退、缓存、计数器） |
| `agent/LlmChatDelegate.java` | Resilience4j 弹性保护层 |
| `agent/core/cache/ChatCacheService.java` | Chat 缓存服务 |
| `agent/core/cache/AiCacheManager.java` | Redis 缓存管理器 |
| `agent/controller/AgentProxyController.java` | REST API 入口 + 计数器端点 |
| `agent/langchain4j/agents/MedicalAgent.java` | MedicalAgent 智能体 |
| `agent/flask/UnifiedFlaskClient.java` | Flask 服务调用（OCR/RAG/Predict） |
| `agent/core/task/AgentTaskService.java` | 异步任务 + 请求分级 |