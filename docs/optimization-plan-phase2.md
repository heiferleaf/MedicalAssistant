# Phase 2 优化执行计划 (2026-05-21 ~ 2026-05-27)

## 概述

基于 Phase 1 (2026-05-15 ~ 2026-05-20) 的成果 —— **吞吐量 82.6× 提升、500并发 100% 成功率**，
Phase 2 将专注于三个方向的深化优化：

1. **aiExecutor 进阶优化** - 进一步降低 p99 和最大等待时间
2. **链路追踪端到端验证** - 确保可观测性数据完整、准确
3. **微服务联调测试** - 验证分布式迁移的稳定性

---

## 目标指标

| 指标 | Phase 1 | Phase 2 目标 | 优先级 |
|------|---------|-----------|--------|
| SSE 50并发 max 等待 | 58.7s | < 45s (进一步优化) | P2 |
| Agent Chat p99 (500t) | 649ms | < 500ms (改善 23%) | P2 |
| 链路追踪完整性 | 50% (待验证) | 95%+ (补齐缺失 span) | P2 |
| 内部 RPC 延迟 p50 | TBD | < 10ms (同机房预期) | P2 |

---

## 任务分解

### Task 1: aiExecutor core-size 对标测试

**所有者**: 后端性能  
**优先级**: P2  
**工作量**: 4h  
**验收标准**: 生成对比报告，确定最优 core-size

#### 1.1 测试设计

```
测试维度:
┌─ core-size: [20, 30, 50]
├─ 并发度: 50 (已知稳定)
├─ LLM 延迟: 20s (模拟真实链路)
└─ 重复轮数: 2 (取平均值)

衡量指标:
├─ 连接持续时间 (mean / max / p95)
├─ 心跳精度 (15±3s 准确率)
├─ 队列等待时间分布
├─ CPU/内存占用
└─ 吞吐量 (ok req/s)
```

#### 1.2 实现步骤

- [ ] **D1 (周三)**: 
  - 准备 test/perf/aiexecutor_coresize_benchmark.py (已完成)
  - 准备 run_aiexecutor_series.sh 自动化脚本 (已完成)
  
- [ ] **D2 (周四)**:
  - 启动应用，验证 SSE 心跳功能正常
  - 执行 core-size=20 基线测试，记录完整指标
  - 执行 core-size=30 对标测试
  - 执行 core-size=50 上界测试
  
- [ ] **D3 (周五)**:
  - 收集所有测试日志和指标
  - 生成对标对比报告 (aiexecutor_benchmark_comparison.md)
  - 确定最优 core-size，提出修改建议

#### 1.3 测试配置

```yaml
# application.yaml 临时变更（测试期间）
infra:
  async:
    ai:
      core-size: ${TEST_CORE_SIZE:20}  # 通过环境变量覆盖
      max-size: 100
      queue-capacity: 500

agent:
  mock:
    enabled: true
    response-delay-ms: 20000  # 模拟 20s LLM 延迟
```

#### 1.4 执行命令

```bash
# 方式 A: 直接运行 Python 脚本（假设应用已启动）
cd /Users/mac/Desktop/project/MedicalAssistant
python3 test/perf/aiexecutor_coresize_benchmark.py

# 方式 B: 自动化系列测试（可选）
bash test/perf/run_aiexecutor_series.sh all

# 方式 C: 单个 core-size 测试
bash test/perf/run_aiexecutor_series.sh 30
```

#### 1.5 预期结果

```
# 对比表格（示例）

| core-size | 成功率 | mean(s) | max(s) | max_减少 | p95(s) | hb_avg |
|-----------|--------|---------|--------|---------|--------|--------|
| 20        | 100%   | 35.5    | 58.7   | baseline| 52.1   | 1.8    |
| 30        | 100%   | 33.2    | 42.1   | -28%    | 38.5   | 1.4    |
| 50        | 100%   | 31.8    | 35.2   | -40%    | 33.1   | 1.2    |
```

**决策逻辑**:
- 如果 core-size=30 时 max ≤ 40s 且无副作用 → 采用 30
- 如果 core-size=50 时 CPU 占用 > 50% → 维持 20，另想办法
- 如果三者效果相近 → 保守采用 30 (平衡性能与资源)

---

### Task 2: 链路追踪端到端验证

**所有者**: 后端可观测性  
**优先级**: P2  
**工作量**: 6h  
**验收标准**: 所有关键路径都有有效 span，span tag 完整

#### 2.1 追踪路径清单

```
🔍 关键路径（需验证）:

1️⃣  用户消息 → LLM 推理
   user_id → session_id → message → LLM(span) → response
   
2️⃣  RAG 调用链
   query → RAG cache check (span: cache_hit) → Flask RAG (span: provider) → response
   
3️⃣  内部 RPC 调用（若启用）
   Agent → FamilyServiceClient → Family service
   Agent → HealthServiceClient → Health service
   
4️⃣  错误链路
   429 Sentinel 限流 (span: blocked=true)
   504 Agent 超时 (span: timeout=true)
   
5️⃣  SSE 长连接
   connect → queued (span) → heartbeat (span) → message (span) → end (span)
```

#### 2.2 实现步骤

- [ ] **D1 (周三)**:
  - 启动 Zipkin UI: `docker run -d -p 9411:9411 openzipkin/zipkin`
  - 验证应用能正确上报 span (访问 localhost:9411)
  
- [ ] **D2 (周四)**:
  - 执行 100 次 Agent Chat 请求（含不同错误场景）
  - 在 Zipkin UI 中采样 10 个代表性 trace
  - 检查以下 span tag:
    - `user_id` ✓ (应有)
    - `session_id` ✓ (应有)
    - `cache_hit` (RAG 缓存命中标记)
    - `provider_status` (Flask/DashScope HTTP 码)
    - `error` (错误信息)
  
- [ ] **D3 (周五)**:
  - 补齐缺失 span tag (如果发现)
  - 编写 Zipkin 追踪指南 (docs/tracing-guide.md)
  - 验证 SLA 指标是否符合预期

#### 2.3 追踪规范

```java
// 示例：RagService 应上报的 span
Span span = tracer.startSpan("rag.query")
span.tag("user_id", userId)
span.tag("question", question)
span.tag("top_k", topK)
span.tag("cache_hit", cacheHit)  // 必填
span.tag("provider", "flask_rag")
span.tag("provider_status", httpStatus)
span.tag("elapsed_ms", elapsedMs)
if (error) span.tag("error", true).tag("error_kind", errorType)
```

#### 2.4 检查清单

```
□ 打开 Zipkin UI (localhost:9411)
□ 选择 medicalassistant 服务
□ 执行 100+ 请求 (混合成功/失败)
□ 采样 10 个 trace 检查:
  ✓ span 数量完整性
  ✓ duration 分布合理
  ✓ tags 包含业务指标
  ✓ error 链路正确标记
□ 记录缺失 tag/span 清单
□ 评估采样率(当前 100%，生产改 10%)
```

#### 2.5 预期发现与改进

```
可能发现的问题:

1. Cache hit tag 缺失
   → 改进: RagService.query() 补齐 span.tag("cache_hit", xxx)
   
2. 内部 RPC 无 span
   → 改进: AiHttpClient 装配 Sleuth 拦截器
   
3. 心跳 span 过多
   → 改进: 心跳作为子 span，100% 采样时隐藏
   
4. 错误类型不清晰
   → 改进: 统一 error tag 格式
```

---

### Task 3: 微服务联调测试

**所有者**: 分布式团队  
**优先级**: P2  
**工作量**: 8h  
**验收标准**: 内部 RPC 调用成功率 > 99%，延迟 < 20ms p50

#### 3.1 测试环境准备

```yaml
# docker-compose 启用微服务（假设）
services:
  agent-service:
    build: .
    environment:
      FAMILY_SERVICE_URL: http://family-service:8080
      HEALTH_SERVICE_URL: http://health-service:8080
      MEDICATION_SERVICE_URL: http://medication-service:8080
      USER_SERVICE_URL: http://user-service:8080
    
  family-service:
    image: medical-family:latest
    ports:
      - "8081:8080"
  
  health-service:
    image: medical-health:latest
    ports:
      - "8082:8080"
      
  # ... 其他服务
```

#### 3.2 测试用例

```
TC-1: 调用 FamilyServiceClient.getFamilyMembers()
  预期: 200, 返回家庭成员列表
  延迟: p50 < 5ms (同机房)
  
TC-2: 调用 HealthServiceClient.getHealthData()
  预期: 200, 返回健康数据
  延迟: p50 < 10ms
  
TC-3: 调用 MedicationServiceClient.getMedicines()
  预期: 200, 返回药品列表
  延迟: p50 < 5ms
  
TC-4: 并发调用 (100 线程，各服务轮询)
  预期: 成功率 > 99%
  错误: < 1 个 500 错误
  
TC-5: 超时测试 (故意延迟响应 20s)
  预期: 10s 内超时，返回 504
  日志: 正确记录超时事件
```

#### 3.3 实现步骤

- [ ] **D1 (周三)**:
  - 检查 4 个 RPC 客户端代码是否完整
  - 验证 HTTP 连接池配置
  
- [ ] **D2 (周四)**:
  - 启动微服务容器组 (如果可用)
  - 运行基本连通性测试 (TC-1 ~ TC-3)
  - 运行并发压力测试 (TC-4)
  
- [ ] **D3 (周五)**:
  - 运行超时和错误场景测试 (TC-5)
  - 收集 Zipkin 链路追踪数据
  - 生成微服务联调报告

#### 3.4 关键代码验证

```bash
# 检查是否存在必要的客户端
grep -r "FamilyServiceClient\|HealthServiceClient\|MedicationServiceClient\|UserServiceClient" \
  src/main/java/ --include="*.java" | grep "class\|interface"

# 检查 HTTP 连接池配置
grep -A 10 "ai.http" src/main/resources/application.yaml
```

#### 3.5 预期结果

```
✅ 成功标准:
- 所有 TC 通过率 > 99%
- p50 延迟 < 10ms
- Zipkin 中可看到完整的跨服务链路
- 无阻塞错误或连接耗尽

⚠️  风险:
- 服务发现失败 (DNS/网络隔离)
- 连接池枯竭 (max-connections 不够)
- 超时配置不合理 (过短/过长)
```

---

## 时间安排

```
周三 (5-22)
├─ 09:00-10:30  Setup & Verification
│  ├─ 启动应用，验证健康状态
│  ├─ 准备测试环境和脚本
│  └─ Task 1 基线测试 (core-size=20)
├─ 10:30-12:00  Task 2 启动
│  ├─ 部署 Zipkin
│  ├─ 首批 100 请求，采集链路数据
│  └─ 初步分析
└─ 14:00-16:30  Task 1 对标测试
   ├─ core-size=30 测试
   └─ core-size=50 测试

周四 (5-23)
├─ 09:00-12:00  Task 1 结果分析
│  ├─ 数据整理和对比
│  ├─ 确定最优值
│  └─ 生成报告
├─ 13:00-15:30  Task 2 深度验证
│  ├─ Zipkin UI 人工审查 (10 个 trace)
│  ├─ 补齐缺失 span tag
│  └─ 性能基线测试
└─ 15:30-17:00  Task 3 准备
   ├─ 启动微服务容器
   ├─ 运行基本连通性测试
   └─ 并发压力测试

周五 (5-24)
├─ 09:00-11:00  Task 3 完成
│  ├─ 超时/错误测试
│  ├─ Zipkin 链路验证
│  └─ 联调报告
├─ 11:00-12:00  汇总
│  ├─ 3 个 Task 总结
│  ├─ 关键发现和建议
│  └─ 生成 Phase 2 总结报告
└─ 13:00-15:00  集成测试
   ├─ 500 并发压测 (验证 Phase 1 成果)
   ├─ 混合场景测试 (SSE + RPC)
   └─ 最终确认
```

---

## 交付物清单

### Task 1 产出
- [ ] `test/perf/aiexecutor_coresize_benchmark.py` (已完成)
- [ ] `test/perf/run_aiexecutor_series.sh` (已完成)
- [ ] `test/perf/results_aiexecutor_*.json` (测试数据)
- [ ] `docs/aiexecutor_benchmark_report.md` (对比报告和结论)

### Task 2 产出
- [ ] `docs/tracing-guide.md` (Zipkin 使用指南)
- [ ] 补齐的 span tag (代码改进)
- [ ] `test/perf/zipkin_validation_report.md` (追踪完整性报告)

### Task 3 产出
- [ ] 微服务联调测试脚本
- [ ] `docs/microservice-integration-report.md` (联调报告)
- [ ] 内部 RPC 延迟基线数据

### Phase 2 总结产出
- [ ] `docs/phase2-summary-2026-05-24.md` (全阶段总结)
- [ ] 决策文档 (确定下一阶段方向)

---

## 关键决策点

### D1: core-size 最优值
**条件**: Task 1 完成后  
**选项**:
- A: 保持 20 (保守策略)
- B: 升至 30 (平衡策略) ← 推荐
- C: 升至 50 (激进策略)

**判断标准**:
- 如果 max 等待 < 40s → 选 B
- 如果 CPU占用 > 40% at 50 concurrent → 选 A
- 如果 max 等待 < 35s 且 CPU < 30% → 选 C

### D2: 链路追踪采样率
**条件**: Task 2 完成后  
**选项**:
- A: 100% (当前) → 生产环保持 10%
- B: 按用户级别差异化 (VIP=100%, 普通=10%)
- C: 按时间差异化 (业务高峰=10%, 非高峰=100%)

**推荐**: A (简单有效，通过环境变量控制)

### D3: 微服务部署时机
**条件**: Task 3 完成后  
**选项**:
- A: 灰度部署 10% (5月底)
- B: 全量部署 (6月初)
- C: 延期 (发现重大问题)

**推荐**: 依 Task 3 结果定，成功率 > 99.5% 时选 A

---

## 风险和应急

| 风险 | 影响 | 应急方案 |
|------|------|---------|
| 应用启动失败 | Task 1-2 无法进行 | 回滚到上一个稳定版本，从 git 历史恢复 |
| Zipkin 部署失败 | Task 2 延迟 | 改用 Jaeger (兼容 Zipkin API) 或使用日志分析 |
| 微服务不可用 | Task 3 无法进行 | 使用 mock 服务模拟 RPC 调用 |
| 测试脚本 bug | 数据不可信 | 人工烟测 3-5 个请求，验证指标合理性 |
| 性能下降 | 无法推进优化 | 逐项回滚最近的改动，定位问题原因 |

---

## 审查与反馈

- **周五 17:00** 内部 review (后端团队)
- **下周一** 汇总反馈，决定下一阶段方向
- **下周二** 更新文档和基线配置

---

**计划审视人**: 后端架构  
**最后更新**: 2026-05-21  
**下一版本**: Phase 3 (分布式事务、容错验证)
