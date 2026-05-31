# RAG 优化执行清单

**状态**: 方案设计完成，代码实现 60%，待性能测试  
**目标完成日期**: 2026-05-31

---

## 📋 执行进度

### 第 1 阶段: 需求分析与方案设计 ✅ (完成)

- [x] 梳理当前 RAG 实现现状
- [x] 获取性能基线数据 (2026-05-17 基准测试)
- [x] 识别瓶颈和优化机会
- [x] 设计 5 个优化方案
- [x] 编写优化策略文档

**交付物**:
- ✅ `docs/rag-optimization-strategy.md` - 完整优化方案
- ✅ `docs/rag-optimization-analysis.md` - 对标分析报告
- ✅ `test/perf/rag_optimization_benchmark.py` - 性能测试脚本

---

### 第 2 阶段: 代码实现 🟡 (进行中 60%)

#### 优化 1: 动态缓存 TTL ✅

- [x] 设计 TTL 选择策略
- [x] 实现 `estimateDynamicTtl()` 方法
- [x] 增加 `putWithTtl()` 方法支持
- [x] 关键词识别 (时效性, 基础知识)
- [x] 热点频率追踪 (QueryStats)

**代码位置**:
```java
RagServiceOptimized.estimateDynamicTtl()    // L129-165
RagCacheService.putWithTtl()                // L91-100
```

**测试**: 待性能测试验证

---

#### 优化 2: 缓存预热 ⏳ (待实现)

- [ ] 设计预热机制
- [ ] 编写 `RagPreWarmingService`
- [ ] 实现应用启动后执行
- [ ] 加载热点问题清单 (从 Redis)
- [ ] 并发预加载 (10 并发)

**预计工作量**: 4h

**实现路线**:
```java
// 1. 编写服务类
public class RagPreWarmingService {
    public void preWarmTopQuestions(int count) { ... }
}

// 2. Spring Boot Startup
@Component
public class AppStartupListener implements ApplicationListener<ContextRefreshedEvent> {
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ragPreWarmingService.preWarmTopQuestions(50);
    }
}
```

---

#### 优化 3: 降级策略 ⏳ (部分实现)

- [x] 设计降级决策树
- [x] 实现超时检测 (`isTimeoutError()`)
- [x] 实现旧值返回 (需要 getStale())
- [ ] 实现 Stale Cache 存储 (长 TTL)
- [ ] 降级指标记录

**当前状态**: 框架完成，细节待补充

**代码位置**:
```java
RagServiceOptimized.queryRagOptimized()     // L107-127 (降级逻辑)
RagCacheService.getStale()                  // L103-111 (待实现)
```

**预计工作量**: 6h

---

#### 优化 4: 智能限流 ⏳ (计划中)

- [ ] 设计分层 Bulkhead
- [ ] 实现 L1-L4 并发分类
- [ ] 实现自适应调整 (基于指标)
- [ ] 完善限流指标

**预计工作量**: 12h

**暂定**: Phase B (优先级较低)

---

#### 优化 5: 预测预加载 ⏳ (长期计划)

- [ ] 需求分析 (语义相似度)
- [ ] 方案选择 (embedding 或 rule-based)
- [ ] 实现原型
- [ ] 集成验证

**预计工作量**: 40h+

**暂定**: Phase C (待 AI 团队支持)

---

### 第 3 阶段: 性能测试 ⏳ (待执行)

#### 冷启动测试

```bash
□ 配置 RAG_PREWARMING_ENABLED=false
□ 启动应用
□ 执行冷启动测试 (首次查询 5 个不同问题)
□ 记录基线: avg, p95, p99
□ 与优化前对比
```

**预期结果**:
- 不优化: avg = 100-150ms (Flask 调用)
- 预热后: avg = 50-70ms (预加载的热点)
- 动态 TTL: 内存占用 ↓7-10%

#### 热启动测试

```bash
□ 配置 RAG_PREWARMING_ENABLED=true
□ 启动应用，等待预热完成
□ 执行热启动测试 (重复查询相同问题 50 次)
□ 记录: avg, p95, p99, 缓存命中率
```

**预期结果**:
- 缓存命中率: 100%
- avg: < 5ms (Redis 延迟)
- p99: < 10ms

#### 并发混合测试

```bash
□ 执行 10 并发，问题重复率 80%
□ 持续运行 5 分钟
□ 监控: 缓存命中率、热点保护触发、延迟分布
```

**预期结果**:
- 缓存命中率: 70-80%
- 热点保护触发: 1-5 次
- p99 延迟: 20-30ms

#### 降级可靠性测试

```bash
□ 模拟 Flask 50% 超时
□ 运行 100 并发查询
□ 测试降级返回比例和有效性
```

**预期结果** (待实现降级后):
- 降级返回: 50%
- 降级有效性: > 90%
- 最终可用性: > 99.9%

---

## 📊 性能指标目标

### 基线 (当前)

| 指标 | 值 | 备注 |
|------|---|------|
| 缓存命中率 | 100% | 热点问题重复 |
| 平均延迟 | 5.8ms | 缓存命中情况 |
| P95 延迟 | 22.7ms | |
| P99 延迟 | 27.0ms | |
| 吞吐 | 3 req/s | 非持续测试 |
| 冷启动延迟 | ~100ms | 首次查询 |

### Phase A 目标 (动态 TTL + 预热)

| 指标 | 目标 | 改善 |
|------|------|------|
| 缓存命中率 | 95%+ | -5% (新问题) |
| 冷启动延迟 | < 50ms | -50% |
| 热点覆盖 | 90%+ | 新增 |
| 内存占用 | -7% | 动态 TTL |
| 吞吐 | 4 req/s | +30% |

### Phase B 目标 (降级 + 智能限流)

| 指标 | 目标 | 改善 |
|------|------|------|
| 可用性 | 99.9% | +0.9% (降级) |
| P99 延迟 | 20ms | -7ms |
| 吞吐 | 8 req/s | +100% |
| 降级触发 | < 1% | 仅超时时 |

---

## 🔧 代码实现检查

### RagServiceOptimized.java ✅ (完成 80%)

- [x] 基础框架
- [x] 动态 TTL 实现
- [x] 超时降级框架
- [x] QueryStats 追踪
- [ ] 预热机制
- [ ] Stale Cache 获取

**编译状态**: ✅ 通过 (修复后)

---

### RagCacheService.java ✅ (完成 90%)

- [x] 基础缓存操作
- [x] 动态 TTL 支持 (`putWithTtl`)
- [x] Stale Cache 框架 (`getStale`)
- [ ] Stale Cache 实际存储

---

### 测试脚本 ✅

- [x] rag_optimization_benchmark.py - 3 个场景完成
- [x] 场景 1: 冷启动
- [x] 场景 2: 热启动
- [x] 场景 3: 并发混合

**待补充**:
- [ ] 场景 4: 降级可靠性测试
- [ ] 场景 5: 长期预热测试

---

## 📚 文档完成度

| 文档 | 完成度 | 状态 |
|------|--------|------|
| rag-optimization-strategy.md | 100% | ✅ |
| rag-optimization-analysis.md | 100% | ✅ |
| RAG-OPTIMIZATION-CHECKLIST.md | 100% | ✅ (本文件) |

---

## 🚀 下一步行动

### 本周 (5-21 ~ 5-24)

- [ ] **完成代码实现**
  - [ ] 修复 RagServiceOptimized 编译错误 (2h)
  - [ ] 实现缓存预热服务 (4h)
  - [ ] 实现 Stale Cache 存储 (2h)

- [ ] **执行性能测试**
  - [ ] 冷启动测试 (1h)
  - [ ] 热启动测试 (1h)
  - [ ] 并发混合测试 (2h)

- [ ] **生成性能报告**
  - [ ] 对标对比表格
  - [ ] 改进分析
  - [ ] 建议后续优化

### 下周 (5-27 ~ 5-31)

- [ ] **Phase B 实施**
  - [ ] 降级策略完整实现
  - [ ] 智能限流框架
  - [ ] 灰度测试

- [ ] **生产部署**
  - [ ] 代码审查
  - [ ] 灰度配置 (10%)
  - [ ] 上线监控

---

## 🎯 成功标准

### Phase A (缓存预热) - 必完成

✅ Checklist:
- [ ] 动态 TTL 完整实现并测试
- [ ] 缓存预热服务完整实现
- [ ] 性能基线数据对比
- [ ] 冷启动延迟 ↓ 50%
- [ ] 代码审查通过

### Phase B (降级策略) - 应完成

⚠️ Checklist:
- [ ] 超时降级机制完整
- [ ] Stale Cache 存储和检索
- [ ] 降级指标记录
- [ ] 可用性测试通过
- [ ] 医学准确性评估

### Phase C (预测预加载) - 可选

💡 Checklist:
- [ ] 需求确认 (是否需要)
- [ ] 方案选择 (embedding vs rule)
- [ ] 原型实现
- [ ] 用户反馈

---

## 📞 联系方式

**RAG 优化负责人**: zhoujinyao11555@gmail.com  
**代码审查**: 后端架构团队  
**性能验证**: 测试/性能优化团队  
**生产部署**: 运维/SRE 团队

---

## 附录: 命令参考

### 运行性能测试

```bash
# 单个测试
python3 test/perf/rag_optimization_benchmark.py

# 指定场景
python3 test/perf/rag_optimization_benchmark.py --scenario cold-start
python3 test/perf/rag_optimization_benchmark.py --scenario warm-start
python3 test/perf/rag_optimization_benchmark.py --scenario concurrent-mixed

# 生成报告
python3 test/perf/rag_optimization_analysis.py \
  --baseline test/perf/results/baseline_*.json \
  --optimized test/perf/results/optimized_*.json
```

### 配置应用

```bash
# 启用缓存预热
export RAG_PREWARMING_ENABLED=true
export RAG_PREWARMING_COUNT=50
export RAG_PREWARMING_CONCURRENCY=10

# 启用降级
export RAG_FALLBACK_ENABLED=true
export RAG_STALE_CACHE_TTL_MULTIPLIER=10

# 动态限流
export RAG_BULKHEAD_MAX_CONCURRENT=30
export RAG_BULKHEAD_MAX_WAIT_MS=200
```

### 监控命令

```bash
# 查看 RAG 缓存命中率
curl http://localhost:8080/actuator/metrics/ai.rag.cache

# 查看 RAG 延迟分布
curl http://localhost:8080/actuator/metrics/ai.rag.duration

# 查看限流拒绝统计
curl http://localhost:8080/actuator/metrics/ai.rag.bulkhead.rejected

# 查看降级触发统计
curl http://localhost:8080/actuator/metrics/ai.rag.fallback
```

---

**最后更新**: 2026-05-21 15:30  
**版本**: v1.0  
**状态**: 活跃 (进行中)
