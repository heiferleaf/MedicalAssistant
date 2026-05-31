# RAG 治理与修复计划

## 1. 背景和目标

当前 RAG 能力已经有基础接口：

- `POST /api/rag/query`
- `RagService -> Flask /rag/query`
- `RagTool -> RagService`
- `Agent -> RagTool`

但从代码和已有设计文档看，RAG 仍处在“能调通”的阶段，还没有达到高并发、可观测、可降级、可评测的工程化状态。

本计划的目标是把 RAG 从单点同步调用改造成一个稳定的 AI 子能力：

- 所有 RAG 外部调用统一走可配置、可观测的 HTTP client。
- 重复 RAG 问题可以命中缓存，降低 Flask 和 LLM 压力。
- RAG 慢调用不会拖垮核心业务线程。
- RAG 错误能被准确表达，不再全部包装成 HTTP 200。
- RAG 准确率和性能有固定 benchmark 可持续回归。

## 2. 当前代码路径

RAG 相关核心代码：

| 文件 | 当前职责 |
| --- | --- |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagController.java` | 暴露 `/api/rag/query` |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagService.java` | 拼接 Flask RAG URL 并调用外部服务 |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagRequest.java` | RAG 请求 DTO |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagResponse.java` | RAG 响应 DTO |
| `src/main/java/com/whu/medicalbackend/agent/flask/FlaskRagProxyService.java` | 另一套 Flask RAG 代理调用 |
| `src/main/java/com/whu/medicalbackend/agent/langchain4j/tools/rag/RagTool.java` | LangChain4j 工具调用 RAG |
| `src/main/resources/application.yaml` | `flask.base-url`、`rag.service.url`、`flask.timeout-ms` 配置 |
| `test/ai-rag-benchmark/` | AI / RAG benchmark 脚本和样例数据 |

已有文档对 RAG 的要求：

- `docs/high-concurrency-backend-plan.md`：要求 Agent / RAG / OCR / Predict 统一 HTTP client、缓存、请求分级、线程池隔离。
- `docs/分布式迁移概要设计文档.md`：要求所有外部 AI 调用统一接入 HTTP client，支持超时、连接池、指标和降级。
- `docs/分布式迁移需求文档.md`：FR-14 要求外部 AI 调用统一治理；FR-15 要求 AI 请求分级。
- `docs/任务分工和测试文档.md`：模块五要求 RAG / OCR / Predict 重复请求可缓存，AI 慢请求不能占满核心 Web 请求线程。

## 3. 主要不足

### 3.1 RAG 有两套调用路径

当前同时存在：

- `RagService`：使用 `new RestTemplate()` 调用 `${rag.service.url}/rag/query`。
- `FlaskRagProxyService`：使用 `RestClient flaskRestClient` 调用 `/rag/query`。

问题：

- 两套路径行为不一致，后续很难统一超时、重试、熔断、指标、日志和缓存。
- `AgentOrchestratorService` 注入了 `FlaskRagProxyService`，但普通 RAG Controller 和 `RagTool` 走的是 `RagService`。
- 修复一处调用逻辑时，另一处可能继续保留旧问题。

修复方向：

- 保留一个统一入口，例如 `RagService`。
- `RagService` 内部依赖统一的 `AiHttpClient` / `FlaskClient`。
- 删除或降级 `FlaskRagProxyService` 为兼容适配层，最终不再让业务直接依赖它。

### 3.2 `RagService` 直接 `new RestTemplate()`

当前 `RagService` 构造函数里直接创建：

```java
this.restTemplate = new RestTemplate();
this.objectMapper = new ObjectMapper();
```

问题：

- 没有连接池，RAG 高并发时连接复用能力弱。
- 没有独立 connect timeout / read timeout。
- 无法挂统一拦截器记录耗时、错误率、traceId。
- 与已有 `FlaskClientConfig` 提供的 `RestClient` 重复。

修复方向：

- 新建统一客户端，例如 `common/infra/http/AiHttpClient`。
- 底层使用带连接池的 Apache HttpClient + `RestClient`，或 WebClient + Reactor Netty。
- `RagService`、`OcrService`、`PredictService` 禁止再各自 `new RestTemplate()`。

### 3.3 当前 `FlaskClientConfig` 也不是完整连接池方案

`FlaskClientConfig` 虽然提供了 `RestClient`，但底层使用 `SimpleClientHttpRequestFactory`。

问题：

- `SimpleClientHttpRequestFactory` 不提供真正的连接池治理。
- 高并发时仍可能出现 TCP 建连开销大、TIME_WAIT 增多、吞吐上不去。
- 不能满足已有文档里“统一 HTTP client 带连接池、超时、指标”的目标。

修复方向：

- 在 `pom.xml` 增加 Apache HttpClient 5 依赖，或改用 WebClient。
- 推荐短期使用 Apache HttpClient 5，和当前 Servlet / Spring MVC 模型更贴合。
- 配置项统一放在：

```yaml
ai:
  http:
    connect-timeout-ms: 1000
    read-timeout-ms: 15000
    max-connections: 200
    max-connections-per-route: 50
```

### 3.4 Controller 错误语义不准确

`RagController` 捕获异常后仍返回 `ResponseEntity.ok(errorResponse)`。

问题：

- Flask 超时、连接失败、响应解析失败都表现为 HTTP 200。
- 调用方、网关、压测脚本和监控无法直接区分成功与失败。
- benchmark 中只能靠 `success=false` 判断错误，HTTP 层指标失真。

修复方向：

- 参数错误返回 400。
- Flask 超时返回 504。
- Flask 不可用返回 503。
- JSON 解析或协议不兼容返回 502。
- 成功才返回 200。
- 响应体保留 `success=false` 和明确错误码，方便前端展示。

### 3.5 缺少输入校验和请求边界

`RagRequest.question` 当前没有校验。

问题：

- 空问题会打到 Flask。
- 超长问题可能占用 Web 线程、HTTP 连接和模型上下文。
- `with_trace` 默认由调用方决定，生产环境可能意外返回过多调试信息。

修复方向：

- `question` 必填，trim 后长度 1 到 1000。
- 生产环境默认关闭 `with_trace`，仅压测、调试、内部环境开启。
- 对用户请求增加 `requestId / traceId`。

### 3.6 缺少 RAG 结果缓存和热点保护

已有文档明确要求：

- RAG 结果缓存 key 使用 `question + topK + strategy` hash。
- 热点医学问答做预热。
- 缓存统一支持随机 TTL、空值缓存和击穿保护。

当前代码没有 RAG 缓存。

问题：

- 相同问题会重复打 Flask / LLM。
- 热点问题高峰会放大外部服务压力。
- 无法达到“命中缓存 RAG P95 < 500ms”的验收目标。

修复方向：

- 增加 `RagCacheService`。
- 缓存 key 使用规范化问题文本和参数 hash。
- TTL 建议 30 分钟，加 0 到 5 分钟随机抖动。
- 对 Flask 返回的空结果或知识库无答案结果做短 TTL 空值缓存，例如 3 到 5 分钟。
- 使用 Redisson 锁保护缓存重建，避免同一热点问题并发击穿。

### 3.7 缺少限流、bulkhead 和降级

当前 RAG 是同步 HTTP 调用。Flask 慢时，请求线程会一直等待。

问题：

- 高并发下 RAG 慢请求会占用 Tomcat 工作线程。
- AI 流量高峰可能拖慢登录、任务、家庭、健康等核心业务。
- 没有明确的“系统繁忙 / 排队中”返回策略。

修复方向：

- 短期：给 RAG 同步接口增加本地并发阈值，例如 Semaphore bulkhead。
- 中期：复杂 Agent + RAG 工具链进入 `aiExecutor`，队列满时快速拒绝。
- 长期：复杂 RAG / Agent 请求支持异步 taskId，结果通过轮询或 WebSocket 返回。

### 3.8 观测指标不足

当前没有统一记录：

- RAG 调用耗时。
- Flask 错误率。
- 超时率。
- 缓存命中率。
- bulkhead 拒绝次数。

问题：

- 压测后无法判断瓶颈在 Web、AI 线程池、Redis、Flask 还是 LLM。
- 不能支撑 `test/ai-rag-benchmark` 产出的结果分析。

修复方向：

- 使用 Micrometer 记录：
  - `ai.rag.request`
  - `ai.rag.duration`
  - `ai.rag.cache.hit`
  - `ai.rag.cache.miss`
  - `ai.rag.timeout`
  - `ai.rag.bulkhead.rejected`
- 在日志里统一输出 `traceId`、`questionHash`、`cacheHit`、`elapsedMs`。

### 3.9 RAG DTO 信息不足

`RagRequest` 只有 `question`、`withTrace`、`withTiming`。

问题：

- 文档中提到缓存 key 需要 `question + topK + strategy`，但 DTO 没有 `topK` / `strategy`。
- 后续不同召回策略、知识库版本、返回引用来源都不好表达。

修复方向：

- 扩展 `RagRequest`：
  - `topK`
  - `strategy`
  - `knowledgeBaseVersion`
  - `traceId`
- 扩展 `RagResponse`：
  - `sources`
  - `cacheHit`
  - `elapsedMs`
  - `providerStatus`
  - `errorCode`

## 4. 目标架构

建议目标链路：

```text
RagController / RagTool
        |
        v
RagService
        |
        +--> RagCacheService
        |       |
        |       +--> Redis / local optional cache
        |
        +--> RagBulkhead
        |
        +--> AiHttpClient
                |
                +--> Flask /rag/query
```

关键原则：

- Controller 只做参数校验、HTTP 状态映射和响应封装。
- RagService 负责业务编排：缓存、bulkhead、降级、指标。
- AiHttpClient 负责外部 HTTP：连接池、超时、trace、统一错误。
- RagTool 不直接处理外部异常细节，只消费 RagService 的稳定返回。

## 5. 修复阶段

### 阶段 0：基线和保护

任务：

1. 固定 benchmark 用例集。
2. 使用 `test/ai-rag-benchmark/ai_rag_benchmark.py` 跑 RAG baseline。
3. 记录当前 RAG P50 / P95 / P99、错误率、准确率。
4. 增加 `RagRequest.question` 基础校验。

验收：

- 可以稳定复现当前 RAG 基线。
- 空问题、超长问题不会打到 Flask。

### 阶段 1：统一 RAG 调用入口

任务：

1. 让 `RagService` 依赖统一 Flask client，不再 `new RestTemplate()`。
2. `FlaskRagProxyService` 改为调用 `RagService`，或标记废弃后删除直接使用。
3. `RagTool` 继续只依赖 `RagService`。
4. 清理 `rag.service.url` 和 `flask.base-url` 的重复配置，只保留一套主配置。

验收：

- RAG 外部调用只剩一条主路径。
- 搜索代码不再出现 RAG 模块内 `new RestTemplate()`。

### 阶段 2：统一 HTTP client 和错误映射

任务：

1. 新增 `AiHttpClient`。
2. 将 `FlaskClientConfig` 改成带连接池的 client。
3. 增加外部调用异常类型：
   - `AiProviderTimeoutException`
   - `AiProviderUnavailableException`
   - `AiProviderBadResponseException`
4. `RagController` 根据异常返回 400 / 502 / 503 / 504。

验收：

- Flask 挂掉时 RAG 返回 503。
- Flask 超时时 RAG 返回 504。
- benchmark 中 HTTP 错误率能真实反映外部依赖问题。

### 阶段 3：RAG 缓存和热点保护

任务：

1. 新增 `RagCacheService`。
2. 规范化问题文本：
   - trim。
   - 合并连续空白。
   - 保留中文原文，不做过度改写。
3. key 格式：

```text
ai:rag:v1:{sha256(question|topK|strategy|knowledgeBaseVersion)}
```

4. TTL：
   - 正常答案：30 分钟 + 0 到 5 分钟随机抖动。
   - 空答案 / 无召回：3 到 5 分钟。
5. 使用 Redisson 锁做同 key 重建保护。

验收：

- 重复 RAG 请求第二次开始命中缓存。
- 命中缓存的 RAG P95 < 500ms。
- Flask QPS 在热点重复问题下明显下降。

### 阶段 4：限流、bulkhead 和线程隔离

任务：

1. 给 `/api/rag/query` 同步接口增加并发上限。
2. 限流触发时返回 429 或业务错误“系统繁忙，请稍后重试”。
3. Agent 中复杂 RAG 工具链调用进入 `aiExecutor`。
4. `AgentProxyController` 的 SSE 执行指定 `aiExecutor`，不再使用默认 `CompletableFuture` 公共线程池。

验收：

- AI 高峰时核心业务接口不被明显拖慢。
- AI 线程池满载时表现为受控拒绝，而不是请求线程被拖死。

### 阶段 5：观测和评测闭环

任务：

1. 增加 Micrometer 指标。
2. 增加结构化日志字段。
3. 扩展 benchmark 用例集：
   - 常见药物副作用。
   - 用药禁忌。
   - 慢病管理。
   - 无答案边界问题。
4. 每次改动后保存 benchmark 报告。

验收：

- RAG accuracy、error_rate、P95、P99 可持续对比。
- 能区分错误来自缓存、Web、Flask、LLM 或知识库。

## 6. 建议代码改动清单

### 新增

| 文件 | 用途 |
| --- | --- |
| `src/main/java/com/whu/medicalbackend/common/infra/http/AiHttpClient.java` | 统一 AI HTTP 调用入口 |
| `src/main/java/com/whu/medicalbackend/common/infra/http/AiHttpClientConfig.java` | 连接池、超时配置 |
| `src/main/java/com/whu/medicalbackend/common/infra/http/AiHttpException.java` | 外部 AI 调用异常基类 |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagCacheService.java` | RAG 缓存读写和 key 规范化 |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagMetrics.java` | RAG 指标封装 |
| `src/main/java/com/whu/medicalbackend/agent/rag/RagProperties.java` | RAG 参数配置 |

### 修改

| 文件 | 改动 |
| --- | --- |
| `RagService.java` | 统一调用 `AiHttpClient`，增加缓存、限流、指标、降级 |
| `RagController.java` | 参数校验和 HTTP 状态码映射 |
| `RagRequest.java` | 增加 `topK`、`strategy`、`traceId` |
| `RagResponse.java` | 增加 `cacheHit`、`elapsedMs`、`sources`、`errorCode` |
| `RagTool.java` | 使用稳定错误语义，返回更清晰的降级文案 |
| `FlaskRagProxyService.java` | 合并到 `RagService` 或转为兼容适配 |
| `application.yaml` | 增加 `ai.http.*`、`ai.rag.cache.*`、`ai.rag.bulkhead.*` |
| `test/ai-rag-benchmark/*` | 增加正式 RAG gold set 和性能阈值说明 |

## 7. 配置建议

```yaml
ai:
  http:
    connect-timeout-ms: 1000
    read-timeout-ms: 15000
    max-connections: 200
    max-connections-per-route: 50
  rag:
    enabled: true
    cache:
      enabled: true
      ttl-seconds: 1800
      ttl-jitter-seconds: 300
      null-ttl-seconds: 300
    bulkhead:
      max-concurrent: 20
      max-wait-ms: 100
    request:
      max-question-length: 1000
      default-top-k: 5
      default-strategy: default
```

## 8. 验收指标

功能指标：

- `/api/rag/query` 正常问题返回 `success=true`。
- 空问题返回 400。
- Flask 不可用返回 503。
- Flask 超时返回 504。
- RAG Tool 在 Agent 中返回清晰降级文案。

性能指标：

| 指标 | 目标 |
| --- | --- |
| 命中缓存 RAG P95 | < 500ms |
| RAG 外部调用超时率 | 可观测，且不拖慢核心业务 |
| RAG 错误率 | benchmark 中单独统计 |
| 缓存命中率 | 热点问题 > 80% |
| bulkhead 拒绝 | 返回明确“系统繁忙” |

评测指标：

- 使用 `test/ai-rag-benchmark/ai_rag_benchmark.py` 生成 JSON / CSV 报告。
- RAG accuracy、error_rate、P95、P99 每次修复前后对比。
- AI 场景不和核心业务压测混算。

## 9. 推荐实施顺序

优先级从高到低：

1. 统一 RAG 调用路径，去掉 `RagService` 里的 `new RestTemplate()`。
2. 引入带连接池和超时的统一 `AiHttpClient`。
3. 修正 Controller 错误状态码和输入校验。
4. 增加 RAG 缓存和热点保护。
5. 增加 bulkhead、限流和线程隔离。
6. 增加 Micrometer 指标和 benchmark 回归。
7. 扩展 RAG DTO，支持 topK、strategy、source 和知识库版本。

这个顺序的理由是：先统一调用入口，再加连接池和错误语义，之后缓存和限流才有稳定的落点；最后再扩展协议和评测集，风险最低。

## 10. 第一轮修复建议

第一轮建议控制在小范围内，先做可回归的工程基础：

1. 修改 `RagService` 构造函数，注入 `RestClient flaskRestClient` 和 Spring 管理的 `ObjectMapper`。
2. 删除 `RagService` 内部 `new RestTemplate()` 和 `new ObjectMapper()`。
3. 在 `RagController` 增加 `question` 空值校验。
4. 增加 RAG 调用耗时日志，字段包含 `questionHash`、`elapsedMs`、`success`。
5. 用 benchmark 脚本跑一份 baseline。

第一轮不建议同时上缓存、异步任务和复杂熔断。先把调用路径收口，后面的优化会更稳。

### 10.1 本轮已完成

本轮已按第一轮修复建议完成以下代码改动：

- `RagService` 不再直接 `new RestTemplate()`，改为注入 Spring 管理的 `RestClient flaskRestClient`。
- `RagService` 统一调用 Flask `/rag/query`，并增加 `question` 空值、空白和最大长度校验。
- `RagService` 增加 RAG 调用耗时日志，记录 `questionHash`、`elapsedMs`、`success`。
- 新增 `RagServiceException`，用于承载 RAG 错误码和 HTTP 状态码。
- `RagController` 不再把所有异常都包装成 HTTP 200，而是按异常返回 400 / 502 / 503 / 504 / 500。
- `FlaskRagProxyService` 改为委托 `RagService`，避免 RAG 继续存在两套外部调用逻辑。

已验证：

```bash
/Applications/IntelliJ\ IDEA.app/Contents/plugins/maven/lib/maven3/bin/mvn -q -DskipTests compile
```

后续仍未完成：

- 底层 `FlaskClientConfig` 还需要从 `SimpleClientHttpRequestFactory` 升级为真正的连接池 HTTP client。
- 需要在可访问 Flask / DashScope 的环境中运行 `test/ai-rag-benchmark` 形成真实 baseline。

### 10.2 响应速度与准确率优化已完成

在第一轮调用路径收口后，本轮继续补齐了 RAG 的速度和准确率支撑能力：

- 扩展 `RagRequest`，支持 `top_k`、`strategy`、`knowledge_base_version`、`trace_id`。
- 扩展 `RagResponse`，支持 `sources`、`cacheHit`、`elapsedMs`、`errorCode`、`providerStatus`。
- 新增 `RagProperties`，把 RAG 请求默认参数、缓存 TTL、热点锁、bulkhead 并发阈值收口到配置。
- 新增 `RagCacheService`，使用 Redis 缓存 RAG 结果，缓存 key 使用规范化问题、`topK`、`strategy`、`knowledgeBaseVersion` 的 SHA-256。
- 增加热点重建保护，同一个 RAG cache key 通过 Redisson 锁避免并发击穿。
- `RagService` 增加本地 Semaphore bulkhead，超过并发上限时返回 `RAG_BUSY` / HTTP 429。
- `RagService` 增加 Micrometer 指标：`ai.rag.request` 和 `ai.rag.duration`，按 result 和 cache hit/miss 打标签。
- `application.yaml` 增加 `ai.rag.request`、`ai.rag.cache`、`ai.rag.bulkhead` 默认配置。
- benchmark 样例 RAG 用例已补充 `top_k=5` 和 `strategy=hybrid`，保证准确率评测参数固定。

这些改动对响应速度的直接收益：

- 相同问题命中 Redis 后不再访问 Flask，目标是命中缓存 RAG P95 < 500ms。
- 热点问题并发时只有少量请求会回源，其余请求等待缓存重建或受控返回。
- bulkhead 能阻止 RAG 慢调用无限占用 Web 请求线程。

这些改动对准确率的直接收益：

- `top_k`、`strategy`、`knowledge_base_version` 显式进入请求和缓存 key，避免不同召回策略混用缓存。
- benchmark 固定检索参数后，准确率结果可复现、可对比。
- `sources` 字段为后续做引用来源核验和人工标注评测预留了协议位置。

后续仍未完成：

- 底层 `FlaskClientConfig` 还需要从 `SimpleClientHttpRequestFactory` 升级为真正的连接池 HTTP client。
- RAG 缓存预热入口、缓存命中率看板、正式 gold set 仍需补齐。
- 需要在可访问 Flask / DashScope 的环境中运行 `test/ai-rag-benchmark` 形成真实 baseline。

### 10.3 本轮架构修复补充

本轮继续针对 RAG / Agent 的响应速度、压力隔离和评测可观测性做了代码级修复：

- 新增 `common/infra/http/AiHttpClient`，统一封装 Flask AI 服务的 JSON、multipart、GET 调用。
- `FlaskClientConfig` 已从 `SimpleClientHttpRequestFactory` 升级为 Apache HttpClient 5 连接池。
- `application.yaml` 新增 `ai.http.*` 配置，支持连接超时、读超时、连接池总数、单路由连接数和连接获取超时。
- `RagService` 改为通过 `AiHttpClient` 访问 `/rag/query`，并在缓存关闭时直接走 bulkhead + provider，不再强依赖 Redis 锁。
- `RagResponse` 对外补齐 snake_case JSON 字段：`cache_hit`、`elapsed_ms`、`error_code`、`provider_status`。
- `FlaskAgentProxyService`、`PredictService`、`OcrService` 已统一切到 `AiHttpClient`，避免 AI 外部调用继续分散在多个 `new RestTemplate()`。
- `AgentProxyController` 的普通 chat 和 SSE chat 已放入 `aiExecutor`，队列满时返回 429，超时时返回 504。
- `RagService` 补充 `ai.rag.cache`、`ai.rag.timeout`、`ai.rag.bulkhead.rejected` 指标，便于压测后定位缓存、超时和限流问题。
- `test/ai-rag-benchmark/ai_rag_benchmark.py` 已增强输出 `cache_hit_rate`、服务端 `elapsed_ms`、`provider_status`、`error_code`，同时保留 accuracy、error_rate、P95/P99、throughput。

本轮测试口径：

```bash
# 准确度 / 基础延迟
python3 test/ai-rag-benchmark/ai_rag_benchmark.py \
  --base-url http://127.0.0.1:80 \
  --cases test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl \
  --concurrency 1 \
  --repeat 1 \
  --warmup 1

# 压力 / P95 / 缓存命中
python3 test/ai-rag-benchmark/ai_rag_benchmark.py \
  --base-url http://127.0.0.1:80 \
  --cases test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl \
  --concurrency 8 \
  --repeat 5 \
  --timeout 180
```

后续建议：

- 把 `ai.rag.cache`、`ai.rag.duration`、`ai.rag.bulkhead.rejected` 接入 Prometheus / Grafana。
- 基于正式 gold set 扩充 RAG 用例到 100 条以上，当前样例集只适合 smoke / baseline。
- 若 Flask RAG 仍是瓶颈，再增加 RAG 缓存预热接口和异步 taskId 模式。
