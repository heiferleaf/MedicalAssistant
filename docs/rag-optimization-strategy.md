# RAG 优化策略与性能对标

**文档日期**: 2026-05-21  
**状态**: 优化方案设计 (待实施)  
**优先级**: P1 (后续性能提升的核心)

---

## 执行摘要

RAG 是医疗助手的知识检索核心。当前实现已有缓存、限流、热点保护等基础优化，但仍有进一步优化空间：

| 维度 | 当前状态 | 优化目标 | 预期收益 |
|------|---------|---------|---------|
| **缓存命中率** | ~100% (热点) | 95%+ (全局) | 热点问题 p99 < 10ms |
| **冷启动延迟** | ~100ms avg | < 50ms | 首次查询体验改善 |
| **并发吞吐** | 3-5 req/s | 10+ req/s | 支持更多并发 |
| **降级可靠性** | 无降级 | 有降级 | 超时返回旧值 |
| **预热有效性** | 无预热 | 智能预热 | 热启动覆盖率 80%+ |

---

## 当前 RAG 架构评估

### 现状分析

```
RagController
    ↓
RagService (已优化)
    ├─ 缓存检查 (Redis)
    ├─ 热点保护 (Redisson Lock)
    ├─ Bulkhead 限流 (Semaphore)
    ├─ 错误处理 (HTTP 状态码)
    └─ 指标记录 (Micrometer)
    ↓
AiHttpClient (连接池)
    ↓
Flask RAG 服务
```

### 性能基线 (2026-05-17)

```
总请求数: 115
成功率: 98.26%
缓存命中率: 100% (RAG 端点)

按端点统计:
┌─ RAG (85 req)
│  ├─ 成功率: 100%
│  ├─ avg: 5.8ms
│  ├─ p95: 22.7ms
│  ├─ p99: 27.0ms
│  └─ 缓存命中: 100%
│
├─ Agent (15 req)
│  ├─ 成功率: 86.7%
│  ├─ avg: 9276ms
│  ├─ p99: 27831ms
│  └─ 包含 LLM 推理延迟
│
└─ Predict (15 req)
   ├─ 成功率: 100%
   ├─ avg: 4169ms
   └─ p99: 5572ms
```

### 瓶颈分析

| 问题 | 症状 | 影响 | 优先级 |
|------|------|------|--------|
| **冷启动慢** | 首次问题 p95 > 20ms | 新用户体验 | P2 |
| **缓存预热不足** | 无热点预知 | 缓存命中率 < 80% | P2 |
| **无降级策略** | 超时直接失败 | 可靠性 < 99% | P3 |
| **Bulkhead 粗粒度** | 并发 > 20 时拒绝 | 吞吐 = 3-5 req/s | P2 |
| **缓存 TTL 固定** | 所有问题 1800s | 内存占用高 | P3 |

---

## 优化方案详解

### 优化 1: 动态缓存 TTL (已实现)

**问题**: 所有问题使用固定 1800s TTL，不符合内容时效性

**方案**:

```
TTL 选择策略:
├─ 时效性问题 (含关键字) → 10min TTL
│  └─ 关键字: 最新、最近、指南、新冠、疫情等
├─ 基础药理问题 → 24h TTL
│  └─ 关键字: 副作用、药理、机制等
├─ 答案短 (< 100字) → 30min TTL
│  └─ 可能是无答案情况
├─ 热点问题 (> 10次查询) → 长 TTL 1.5×
└─ 多来源答案 (> 3个源) → 中长 TTL 1.2×
```

**实现**:
- 在 `RagServiceOptimized.estimateDynamicTtl()` 中实现
- 维护 `QueryStats` 统计每个问题的查询频率
- 自动调整 TTL，无需人工干预

**预期效果**:
- 缓存未命中减少 20-30%
- 内存占用降低 15-20%
- 热点问题 p99 < 10ms

---

### 优化 2: 智能热点预热

**问题**: 系统启动时缓存为空，热点问题首次查询慢

**方案**:

```
预热机制:
┌─ 应用启动
├─ 加载热点问题清单 (从 Redis 或配置)
├─ 并发查询 (10 并发)
├─ 填充 Redis 缓存
└─ 预热完成，服务就绪

热点识别:
├─ 线上查询频率 top-50
├─ 医学领域常见问题 (预定义)
└─ 用户反馈高评价问题
```

**实现步骤**:
```java
// 应用启动后执行
@PostConstruct
public void preWarmCache() {
    List<String> hotQuestions = loadHotQuestions();  // 从 Redis 或配置加载
    hotQuestions.parallelStream()
        .limit(50)
        .forEach(q -> {
            RagRequest req = new RagRequest();
            req.setQuestion(q);
            queryRagOptimized(req);  // 预热缓存
        });
    logger.info("RAG cache pre-warming completed, {} questions cached", hotQuestions.size());
}
```

**预期效果**:
- 热启动缓存命中率 > 90%
- 系统启动后 5 分钟内热点问题延迟 < 10ms
- 用户冷启动体验改善 30%

---

### 优化 3: 分层降级策略

**问题**: RAG 超时直接返回错误，用户体验差

**方案**:

```
降级决策树:
RAG 超时 → 检查是否有旧缓存
    ├─ 有旧缓存 → 返回旧值 (标记 stale)
    │   └─ 用户看到"数据可能不最新"
    └─ 无旧缓存 → 返回预设答案 (通用回复)
        └─ 用户看到"系统繁忙，请稍后重试"
```

**实现细节**:

1. **Stale Cache 维护**:
   - 在 Redis 中维护两个 key:
     - `ai:rag:v1:{hash}` → 正常缓存 (TTL = configurable)
     - `ai:rag:v1:stale:{hash}` → 过期值 (TTL = 正常 TTL × 10)
   
2. **超时检测和返回**:
   ```java
   catch (RagServiceException e) {
       if (isTimeoutError(e)) {
           RagResponse stale = ragCacheService.getStale(cacheKey);
           if (stale != null) {
               stale.setProviderStatus("cache_stale");
               meterRegistry.counter("ai.rag.fallback", "reason", "timeout").increment();
               return stale;
           }
       }
       throw e;
   }
   ```

3. **指标记录**:
   - `ai.rag.fallback{reason=timeout}` 计数
   - 追踪降级频率，如果 > 5% 则告警

**预期效果**:
- RAG 可用性 99% → 99.9% (减少超时失败)
- 用户体验 SLA 达成率 > 99.5%
- 灰度发布时有保障

---

### 优化 4: 智能限流与并发优化

**问题**: Bulkhead 最多 20 并发，不够灵活

**方案**:

```
分层 Bulkhead:
┌─ L1: 快速问题 (缓存命中)
│  └─ 无限制 (Redis 响应快)
│
├─ L2: 热点问题 (第一次查询中)
│  ├─ 保护队列大小: 5
│  └─ 等待时间: 100ms
│
├─ L3: 普通问题 (新查询)
│  ├─ 最大并发: 20
│  └─ 等待时间: 200ms
│
└─ L4: 复杂问题 (超过 1000 字)
   ├─ 最大并发: 5
   └─ 队列拒绝: 429
```

**指标驱动的并发调整**:
```
监控每 5 分钟的:
├─ RAG timeout 率
├─ Flask 错误率
├─ 队列深度
└─ p99 延迟

如果 p99 > 50ms → 并发 - 5
如果 p99 < 10ms && timeout < 0.1% → 并发 + 2
```

**预期效果**:
- 吞吐量 3-5 req/s → 10+ req/s
- p99 延迟 < 30ms (从 27ms 保持)
- 错误率 < 0.1%

---

### 优化 5: 缓存预测和智能预加载

**问题**: 用户搜索的问题缺乏预测，无法提前缓存

**方案**:

```
问题相似度检测:
用户输入: "高血压如何治疗"
    ↓
匹配相似问题:
  ├─ "高血压患者应该如何选择降血压药物" (相似度 95%)
  ├─ "高血压的并发症有哪些" (相似度 80%)
  └─ "血压升高的原因" (相似度 75%)
    ↓
预加载 top-3 相似问题的缓存
    ↓
用户点击相关问题时，直接返回缓存结果
```

**实现方式**:
- 使用 LSH (Locality Sensitive Hashing) 或 embedding 距离
- 维护问题相似度矩阵 (定期更新)
- 用户输入时，异步预加载相似问题

**预期效果**:
- 热点相关问题命中率 > 85%
- 避免重复查询 20-30%
- 用户体验中位数延迟 < 5ms

---

## 性能测试计划

### 测试场景

#### 场景 1: 冷启动 (无缓存)

```bash
# 首次查询 5 个不同问题
python3 test/perf/rag_optimization_benchmark.py --scenario cold-start

预期:
├─ 成功率: 100%
├─ 平均延迟: 50-100ms (取决于 Flask 延迟)
├─ p95: 80-120ms
├─ 缓存命中: 0%
└─ 吞吐: 3-5 req/s
```

#### 场景 2: 热启动 (有缓存)

```bash
# 查询相同问题 50 次
python3 test/perf/rag_optimization_benchmark.py --scenario warm-start

预期:
├─ 成功率: 100%
├─ 平均延迟: 5-10ms (缓存命中)
├─ p95: 10-15ms
├─ 缓存命中: > 95%
└─ 吞吐: 100+ req/s
```

#### 场景 3: 并发混合 (热点保护)

```bash
# 10 并发查询，问题重复率 80%
python3 test/perf/rag_optimization_benchmark.py --scenario concurrent-mixed

预期:
├─ 成功率: > 99%
├─ 平均延迟: 15-20ms
├─ p95: 25-30ms
├─ 缓存命中: 70-80%
├─ 热点保护触发: 1-5 次
└─ 吞吐: 10-15 req/s
```

#### 场景 4: 降级可靠性

```bash
# 模拟 Flask 50% 超时，查看降级效果
python3 test/perf/rag_optimization_benchmark.py --scenario fallback-resilience

预期:
├─ 成功率: > 99.5% (降级返回 stale 缓存)
├─ 降级触发: 约 50%
├─ 降级有效性: 95%+ (旧值相关)
└─ 用户体验降级: 可接受 (返回不完全但有用的信息)
```

### 测试执行

```bash
# 执行所有测试场景
bash test/perf/rag_optimization_series.sh

# 生成对比报告
python3 test/perf/rag_optimization_analysis.py \
  --baseline test/perf/results/baseline_*.json \
  --optimized test/perf/results/optimized_*.json
```

---

## 实施路线图

### Phase A: 基础优化 (1 周)

- [x] 动态缓存 TTL (RagServiceOptimized)
- [ ] 缓存预热框架
- [ ] 性能测试脚本

### Phase B: 高级优化 (2 周)

- [ ] 降级策略实现
- [ ] 智能限流调整
- [ ] 指标完善

### Phase C: 验证和优化 (1 周)

- [ ] 性能对标和数据分析
- [ ] 生产部署前灰度测试
- [ ] 文档和培训

---

## 预期性能收益

### 吞吐量提升

```
当前: 3-5 req/s (RAG 端点)

优化后:
├─ Phase A (动态 TTL + 预热): 5-8 req/s (+50%)
├─ Phase B (降级 + 限流): 8-12 req/s (+100%)
└─ Phase C (智能预加载): 12-15 req/s (+150%)
```

### 延迟优化

```
当前平均延迟: 5.8ms (缓存命中情况)
当前 p99: 27ms

优化后:
├─ 缓存命中: 5ms → 3ms (Redis 优化)
├─ 缓存未命中: 100ms → 50ms (预热 + 预测)
├─ p99: 27ms → 15ms (并发优化)
└─ p99.9: TBD → 50ms (降级策略)
```

### 可靠性提升

```
当前可用性: 99%+ (无降级)

优化后:
├─ 无降级可用性: 99%+
├─ 有降级可用性: 99.9%+ (返回旧值)
└─ SLA 达成: 99.95%
```

---

## 配置参数建议

### application.yaml

```yaml
ai:
  rag:
    request:
      max-question-length: 1000
      default-top-k: 5
      max-top-k: 10
      default-strategy: hybrid
    cache:
      enabled: true
      ttl-seconds: 1800          # 基础 TTL: 30 min
      ttl-jitter-seconds: 300    # 抖动: 0-5 min
      null-ttl-seconds: 300      # 无答案 TTL: 5 min
      null-ttl-jitter-seconds: 60
      lock-wait-seconds: 1       # 热点等待: 1s
      lock-lease-seconds: 10     # 锁持续: 10s
    bulkhead:
      max-concurrent: 30         # 并发数: 提升到 30
      max-wait-ms: 200           # 等待: 200ms
    prewarming:
      enabled: true
      hot-questions-count: 50    # 预热 top-50 热点
      prewarming-concurrency: 10
    fallback:
      stale-cache-ttl-multiplier: 10  # 旧值保留 10 倍 TTL
      fallback-enabled: true
```

---

## 监控和告警

### 关键指标

```
ai.rag.request
  ├─ dimensions: [result, cache, success]
  └─ 目标: 成功率 > 99.9%

ai.rag.cache
  ├─ dimensions: [hit/miss]
  └─ 目标: 命中率 > 90%

ai.rag.duration
  ├─ dimensions: [result, cache, success]
  ├─ target p50: < 10ms
  ├─ target p95: < 30ms
  └─ target p99: < 50ms

ai.rag.bulkhead.rejected
  └─ 目标: 每分钟 < 1 次

ai.rag.fallback
  ├─ dimensions: [reason]
  └─ 目标: 触发率 < 1%
```

### 告警规则

| 指标 | 告警阈值 | 等级 |
|------|---------|------|
| RAG 错误率 > 2% | 5 分钟 | P2 |
| RAG p99 > 100ms | 10 分钟 | P3 |
| Bulkhead 拒绝 > 10/min | 1 分钟 | P2 |
| 降级触发率 > 5% | 5 分钟 | P2 |
| 缓存命中率 < 70% | 15 分钟 | P3 |

---

## 总结与建议

### 核心改进点

1. **动态 TTL** - 智能适应问题时效性，减少内存占用 15-20%
2. **缓存预热** - 应用启动时预加载热点，热启动覆盖 > 90%
3. **降级策略** - 超时返回旧值，可用性 99% → 99.9%
4. **智能限流** - 分层控制并发，吞吐提升 150%+
5. **性能预测** - 预加载相似问题，体验提升 30%

### 风险和缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 旧值返回过期信息 | 医学准确性 | 标记 `stale` 标签，可控时间 |
| 内存占用增加 | 缓存爆炸 | 定期清理，TTL 管理 |
| 部署失败 | 服务中断 | 灰度 10%，回滚计划 |

### 优先级顺序

**必做 (Phase A)**:
- 动态 TTL (快速见效)
- 性能测试 (验证收益)

**应做 (Phase B)**:
- 缓存预热 (体验提升)
- 降级策略 (可靠性)

**可做 (Phase C)**:
- 智能限流 (进阶优化)
- 预测预加载 (长期优化)

---

**下一步**: 
1. 评审此方案
2. 启动 Phase A 实施 (预计 1 周)
3. 执行性能测试和基线对比
4. 决定是否继续 Phase B/C

**所有者**: RAG/AI 小组  
**联系方式**: zhoujinyao11555@gmail.com
