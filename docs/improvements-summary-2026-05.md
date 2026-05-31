# 系统改进总结 (2026-05) & 后续优化方向

**报告日期**: 2026-05-21  
**覆盖周期**: 2026-05-15 至 2026-05-21  
**核心成果**: 吞吐量 8 → 661 req/s (82.6×)，500并发 100% 成功率  

---

## 一、已完成的核心优化

### 1.1 网络层优化

#### nginx upstream keepalive
- **问题**: 每个请求创建新 TCP 连接，200 并发时导致端口耗尽→502 洪水
- **修复**: 增加 `upstream keepalive 64;` + `proxy_http_version 1.1` + `Connection ""`
- **效果**: 502 错误完全消失，Health Check 成功率 33% → 100%
- **相关 commit**: 54f19c6

#### SSE 长连接优化
- **proxy_buffering off**: 禁用 nginx 缓冲，token 实时到达，延迟从 20s → 实时
- **15s 心跳**: ScheduledExecutorService 定期发送 keep-alive，防止 nginx 120s/300s 超时断连
- **queued ACK**: 连接建立即回复 queued 事件（3ms），提升用户体验
- **nginx proxy_read_timeout**: 120s → 300s，适配长连接
- **相关 commit**: 54f19c6, 0a9231a

#### SSE 性能验证
- 50 并发长连接，20s LLM 延迟
- 心跳间隔精度: 15.0s ± 0s (200 次采样)
- 全部连接存活率: 100%
- 批次最大等待: 138.5s → 58.7s (core-size=8→20 后 -58%)

### 1.2 应用层限流与流控

#### Sentinel 动态 QPS 注入
- **问题**: `SentinelConfig.java` 硬编码 QPS=10，99% 请求被限流
- **修复**: 改用 `@Value("${agent.sentinel.chat-qps:500}")` 动态注入
- **默认值调整**:
  - chat-qps: 10 → 500
  - stream-qps: 10 → 300  
  - ocr-qps: 10 → 30
- **效果**: Agent Chat ok/s 10 → 430 (200t)
- **环境变量覆盖**:
  ```bash
  SENTINEL_CHAT_QPS=2000       # 压测时放开限制
  SENTINEL_STREAM_QPS=300
  SENTINEL_OCR_QPS=30
  ```
- **相关 commit**: ac9f5bf

#### 应用线程池扩容
- **aiExecutor 参数变更**:
  - core-size: 8 → 20
  - max-size: 20 → 100
  - queue-capacity: 100 → 500
- **问题**: 队列模式(8核填满500队列再建线程) → 50并发时阶梯等待120s
- **修复**: 扩大核心线程数、最大线程数、队列，并发50个请求时最大等待 ~40s
- **效果**: max 连接耗时 138.5s → 58.7s (-58%)
- **相关 commit**: cfc427a, 7075d45

### 1.3 HTTP 状态码修复

#### 错误应答状态码规范化
- **问题**: RejectedExecutionException、超时等通过 HTTP 200 返回，加 JSON 错误体
- **修复**: 改用 `ResponseEntity`，直接返回正确的 HTTP 状态码
  - 429 Sentinel 限流
  - 504 超时
  - 400 参数错误
- **效果**: 客户端可正确识别限流/超时，无需解析 JSON body
- **相关 commit**: cfc427a

### 1.4 数据库性能优化

#### N+1 查询消除
- **getUserSessions**: 原 1 次 SELECT + N 次 SELECT last_message → 改为单 SQL LEFT JOIN，使用行号函数
- **medicine 批量查询**: findByIds() 改用 IN 子句，单 query 代替多次 findById()
- **相关 commit**: c07c12e, edfbcd8

#### HikariCP 连接池扩容
- **maximum-pool-size**: 10 → 40
  - 理由: AI 线程池 max=100，单请求最多 4 条 SQL，留余量给其他模块
- **connection-timeout**: 新增 3000ms
- **相关 commit**: 2367288

### 1.5 缓存策略

#### Redis 会话级缓存
- **getRecentMessages 二级缓存**: 
  - 如果 limit ≤ 20，先查 Redis (TTL=30s)
  - Cache key: `agent:memory:recent:{sessionId}`
  - Cache 失效时: 新增消息自动 evict
- **效果**: LLM 调用时的消息列表不走 DB
- **相关 commit**: 71ac518

#### 会话探针缓存
- **touchSessionCached**: 用 Redis 记录已创建的会话，跳过重复 DB upsert
- **Cache key**: `agent:session:seen:{sessionId}` (TTL=24h)
- **效果**: 每消息减 1 次 INSERT/UPDATE SQL
- **相关 commit**: 12fef39

#### RAG 缓存优化（既有）
- TTL 策略: 1800s + 300s jitter，null 缓存 300s 防止击穿
- bulkhead: 20 并发限制，超过返回 429
- **相关 commit**: e3c98f4

### 1.6 链路追踪集成

#### Sleuth + Zipkin/Jaeger
- **新增 TracingConfig.java**: 自动装配分布式跟踪
- **span 标签**:
  - `user_id`, `session_id` 打入链路
  - `cache_hit`, `provider_status` 等业务指标
- **采样率**: 100% (生产环境按需调整)
- **endpoint**: `http://localhost:9411/api/v2/spans` (zipkin)
- **相关 commit**: 43bd61a

#### 内部 RPC 客户端（微服务支持）
- **新增 4 个内部服务代理**:
  - FamilyServiceClient
  - HealthServiceClient
  - MedicationServiceClient
  - UserServiceClient
- **底层**: 基于统一 AiHttpClient，复用连接池和超时配置
- **用途**: 实现分布式迁移时内部调用

### 1.7 配置管理优化

#### application.yaml 完善
- **ai.http.\***: Flask HTTP 客户端全局配置（连接池、超时、路由级限制）
- **ai.rag.bulkhead**: RAG 并发限制（max=20，wait=100ms）
- **ai.cache**: OCR/RAG/Predict 缓存 TTL 集中管理
- **resilience4j**: 速率限制器和熔断器实例，可按服务粒度控制
- **agent.sentinel.\***: Sentinel 限流阈值（从配置注入）
- **agent.task.grading**: L1-L4 超时分级（2s → 120s）

---

## 二、压测数据对比

### 2.1 关键指标演进 (Agent Chat endpoint @ 500并发)

| 阶段 | 成功率 | avg(ms) | p99(ms) | ok req/s | 主要瓶颈 |
|------|--------|---------|---------|----------|----------|
| Baseline (200t) | 1.1% | 8 | 53 | 8 | nginx 502 |
| R1 nginx 修复 | 1.8% | 1 | 6 | 10 | Sentinel 10 QPS |
| R2 Sentinel 提升 (200t) | 84.2% | 14 | 173 | 431 | executor 队列 |
| R4 executor 扩容→100 (500t) | 73.6% | 165 | 460 | 565 | Sentinel 上限 |
| **R6 最终 (500t, Sentinel=2000)** | **100%** | **260** | **649** | **661** | 无 |

**吞吐量提升**: 8 → 661 req/s = **82.6×**

### 2.2 分布式迁移前后

| 指标 | 单体 | 微服务 | 改善 |
|------|------|--------|------|
| Health Check 成功率 | 33% | **100%** | ✅ |
| 创建会话 p50 | 5ms | 26ms | ⚠ 多跳 (预期) |
| Agent Chat p50 | - | 178ms(500t) | ✓ 可控 |

---

## 三、遗留优化建议 (按优先级)

### P1 (立即处理)
- [x] SSE 心跳测试验证 → **已完成 (2026-05-20)**
- [x] 500并发 100% 成功率 → **已达成 (R6)**

### P2 (近期处理，< 1 周)

#### 1. aiExecutor 进一步优化
- **当前**: core-size=20 时 50 并发 max 等待=58s
- **建议**: core-size=30 或 50，测试 max 等待时间
- **ROI**: 进一步降低 p99，改善用户体验
- **测试计划**:
  ```bash
  # test/perf/ 新增脚本
  core_sizes=(20 30 50)
  for cs in "${core_sizes[@]}"; do
    sed -i "s/core-size: .*/core-size: $cs/" application.yaml
    # 运行 50并发 SSE 测试，记录 max 等待
  done
  ```

#### 2. SSE 断线重连测试
- **当前**: 未测试客户端侧断线和自动重连
- **建议**: 
  - 客户端侧关闭连接，验证后端侧正确回收资源
  - EventSource 自动重连后，是否继续接收消息
  - 验证 Redis session-seen flag 是否正确过期 (24h)
- **预期**: 前端 EventSource 原生支持自动重连，后端无改动需要

#### 3. 链路追踪端到端验证
- **当前**: 配置已完成，未在真实请求中验证
- **建议**:
  - 启动 Zipkin UI (localhost:9411) 或 Jaeger (localhost:16686)
  - 执行 100+ 请求，检查:
    - span 完整性 (所有跨进程调用是否都有 span)
    - 延迟分解 (哪个环节最耗时)
    - 错误跟踪 (429/504 等错误是否正确记录)
  - 补齐缺失的 span tag (特别是 cache_hit, provider_status)
- **关键字段**:
  ```
  user_id, session_id
  cache_hit (RAG 命中率)
  provider_status (Flask/DashScope 响应码)
  operation (RAG/OCR/predict/LLM)
  ```

#### 4. 内部 RPC 调用测试
- **当前**: 客户端代码已生成，未在真实场景测试
- **建议**:
  - 单测: 各微服务容器启动，测试网络连通性
  - 集成测试: LLM 调用时通过内部 RPC 获取用户信息、家庭成员等
  - 性能测试: 内部 RPC 延迟分布 (p50/p99)
  - Zipkin 验证: 是否正确上报跨服务调用

### P3 (优化调优，2 周+)

#### 1. Sentinel 限流策略调整
- **当前**: 全局 QPS 限制 (500)
- **建议**:
  - 按用户级/会话级差异化限流 (VIP 用户提速)
  - 时间段差异化 (夜间降低 QPS 以节约 DashScope 费用)
  - 队列权重调整 (L1 优先级任务优先出队)

#### 2. 缓存 TTL 自适应
- **当前**: RAG 缓存 TTL 固定 1800s
- **建议**: 
  - 按提问类型自适应 (常见问题长 TTL，冷门问题短 TTL)
  - 用户反馈信号 (用户标记答案不对→清空缓存)

#### 3. RAG bulkhead 精细化
- **当前**: 全局 20 并发，超出 429
- **建议**:
  - 区分请求优先级 (L1 绝对不排队，L4 允许排队)
  - 动态 bulkhead (高峰 20 并发，低谷放宽到 50)

---

## 四、后续测试计划

### 周期 1: 即日 - 5.23 (优化器调优)
```
□ 完成 aiExecutor core-size 对标测试
□ SSE 断线重连 e2e 测试
□ 启动 Zipkin，验证链路追踪采集
□ 补齐 span tag，重新运行压测
```

### 周期 2: 5.24 - 5.30 (微服务联调)
```
□ 内部 RPC 单测 (通用网络测试)
□ 集成测试 (真实微服务流程)
□ 微服务性能基线测试
□ 分布式事务一致性测试
```

### 周期 3: 6.1+ (生产阶段)
```
□ 全量压测 (2000+ 并发)
□ 容错验证 (单个服务宕机影响)
□ 金丝雀部署 (灰度 10% 流量)
□ 深度可观测性 (SLA 监控)
```

---

## 五、关键决策清单

| 决策 | 现状 | 建议 | 所有者 |
|------|------|------|--------|
| aiExecutor core-size | 20 | 测试 30/50 | 后端 |
| Sentinel 限流策略 | 全局 QPS | 改进步长粗糙 | 后端 |
| RAG 缓存 TTL | 固定 1800s | 自适应 | AI |
| 链路追踪采样率 | 100% | 生产改 10% | 运维 |
| 内部 RPC 超时 | 10s | 分级: L1=2s, L4=30s | 后端 |

---

## 六、配置黄金标准

### 生产环境推荐配置

```yaml
# application.yaml
infra:
  async:
    ai:
      core-size: 50      # 或根据 P2 优化结果调整
      max-size: 100
      queue-capacity: 500

agent:
  sentinel:
    chat-qps: 500        # 可通过环境变量覆盖为 2000+ (压测)
    stream-qps: 300
    ocr-qps: 30

ai:
  rag:
    bulkhead:
      max-concurrent: 20
      max-wait-ms: 100
    cache:
      enabled: true
      ttl-seconds: 1800
      
management:
  tracing:
    sampling:
      probability: 0.1   # 生产环境改为 10%

# 环境变量 (docker-compose/.env)
SENTINEL_CHAT_QPS=500         # 生产默认
ZIPKIN_HOST=zipkin:9411       # 可选，未部署时默认关闭
```

### 压测环境临时配置

```bash
# docker-compose override
SENTINEL_CHAT_QPS=2000        # 放开限制，观测真实吞吐
AGENT_MOCK_RESPONSE_DELAY_MS=20000  # 压测 LLM 延迟链路
FLASK_URL=http://flask:8001   # 真实 Flask (或 mock)
```

---

## 七、相关文件导航

| 文件 | 改动 | 关键参数 |
|------|------|---------|
| [application.yaml](../src/main/resources/application.yaml) | ✅ 集中配置 | `ai.http.*`, `agent.sentinel.*`, `infra.async.ai` |
| [SentinelConfig.java](../src/main/java/com/whu/medicalbackend/common/config/SentinelConfig.java) | ✅ 动态注入 | `@Value("${agent.sentinel.*}")` |
| [AgentOrchestratorService.java](../src/main/java/com/whu/medicalbackend/agent/AgentOrchestratorService.java) | ✅ 状态码修复 | `ResponseEntity` 返回 429/504 |
| [AgentMemoryRepository.java](../src/main/java/com/whu/medicalbackend/agent/core/memory/AgentMemoryRepository.java) | ✅ Redis 缓存 | `getRecentMessages()`, `session-seen` |
| [AgentProxyController.java](../src/main/java/com/whu/medicalbackend/agent/controller/AgentProxyController.java) | ✅ SSE 心跳 | `ScheduledExecutorService`, 15s 心跳 |
| [nginx.conf](../nginx/nginx.microservices.conf) | ✅ keepalive+buffering | `upstream keepalive 64`, `proxy_buffering off` |
| [TracingConfig.java](../src/main/java/com/whu/medicalbackend/common/config/TracingConfig.java) | ✅ 链路追踪 | Sleuth 自动装配 |
| [test/perf/sse_longconn_test.py](../test/perf/sse_longconn_test.py) | ✅ SSE 测试 | 50 并发，20s LLM 延迟 |
| [test/perf/sse_report_20260520.md](../test/perf/sse_report_20260520.md) | ✅ 测试报告 | 心跳精度、连接存活率 |
| [test/perf/iteration_report_20260520.md](../test/perf/iteration_report_20260520.md) | ✅ 迭代报告 | 82.6× 吞吐提升 |

---

## 八、快速验证清单

运行以下命令验证系统状态：

```bash
# 1. 检查配置是否正确加载
curl -s http://localhost:8080/health | jq '.'

# 2. 运行 SSE 测试（验证心跳）
python3 test/perf/sse_longconn_test.py --concurrency 50 --duration 60

# 3. 查看 Zipkin 链路（需启动 Zipkin)
# 访问 http://localhost:9411 → Find Traces → 选择 medicalassistant 服务

# 4. 检查 Redis 缓存命中率
redis-cli KEYS "agent:memory:*" | wc -l

# 5. 数据库连接池状态
curl -s http://localhost:8080/actuator/metrics | jq '.names[]' | grep hikari
```

---

**生成时间**: 2026-05-21 10:00  
**下一次审查**: 2026-05-27 (P2 优化完成后)  
**所有者**: 后端性能团队
