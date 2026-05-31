# Flask RAG 并发瓶颈 — 实测发现与修复 (已验证)

**日期**: 2026-05-29
**性质**: 真实端到端测试发现的高价值 bug + 已验证修复
**测试环境**: Flask(宿主机, zjy conda 环境) + 真实 DashScope qwen-turbo + 本地 Redis, Neo4j 关闭
**API Key**: sk-e7051a4ddaa049e9bd25c8264dfb3b15 (qwen-turbo, 实测可用)

> 与之前的理论分析文档不同,本报告所有数据均为**实际运行测得**。

---

## 一、TL;DR

Flask AI 服务的三个端点 (`/rag/query`, `/api/predict/analyze`, `/ocr/predict`)
都用 `async def` 声明,但内部调用的是 **同步阻塞** 的 dashscope SDK
(`Generation.call` / `MultiModalConversation.call`)。在单 worker uvicorn 下,
阻塞调用**卡死事件循环**,使所有并发请求被迫串行。

**修复**: `async def` → `def`(FastAPI 自动用线程池跑同步处理器);OCR 因有
`await file.read()` 保留 async,把阻塞 SDK 调用包进 `asyncio.to_thread`。

**实测效果 (RAG, 10 并发)**: 吞吐 **0.20 → 1.06 req/s (5.3×)**,p50 延迟 **35.4s → 6.3s (5.6×)**。

---

## 二、实测数据对比 (15 题, 真实 LLM 调用)

### 修复前 (`async def`, 阻塞事件循环)

| 并发 | 总耗时 | 吞吐 | p50 延迟 | p95 延迟 | 单次 LLM |
|------|--------|------|---------|---------|---------|
| 1 | 69.9s | 0.21 r/s | 4.4s | 8.5s | 4.66s |
| 5 | 71.7s | 0.21 r/s | 22.7s | 28.3s | 4.78s |
| 10 | 73.9s | 0.20 r/s | 35.4s | 54.8s | 4.93s |

**特征**: 并发上升,吞吐不变(卡死 0.2 r/s),延迟线性暴涨 → 典型的串行排队。
单次 LLM 时间稳定 ~4.7s,但总延迟远大于它 → 请求在事件循环后排队。

### 修复后 (`def`, 线程池)

| 并发 | 总耗时 | 吞吐 | p50 延迟 | p95 延迟 | 单次 LLM |
|------|--------|------|---------|---------|---------|
| 1 | 88.8s | 0.17 r/s | 5.9s | 8.0s | 5.92s |
| 5 | 22.1s | **0.68 r/s** | **6.3s** | 8.4s | 6.13s |
| 10 | 14.2s | **1.06 r/s** | **6.3s** | 9.4s | 6.23s |

**特征**: p50 延迟稳定 ~6.3s 不随并发变化(无排队),吞吐随并发线性增长。

> 注: 修复后 Round 1 串行略慢 (88.8s vs 69.9s) 是 DashScope 当次 LLM 延迟波动
> (单次 5.9s vs 4.7s),与修复无关。串行吞吐天然受单次 LLM 时间限制,修复只影响并发。

### 关键提升

| 指标 (10 并发) | 修复前 | 修复后 | 提升 |
|------|--------|--------|------|
| 吞吐 | 0.20 r/s | 1.06 r/s | **5.3×** |
| p50 延迟 | 35.4s | 6.3s | **5.6×** |
| 5 并发总墙钟(隔离测试) | 44.8s | 8.7s | **5.2×** |

---

## 三、根因排查过程(诊断逻辑链)

这个 bug 一开始被多个干扰因素掩盖,排查走了完整的二分定位:

1. **现象**: Flask `/rag/query` 直调,并发 1/5/10 吞吐都是 0.2 r/s,完全不提速。
   → 怀疑 FastAPI 事件循环被阻塞。

2. **排除账号限速**: 直连 DashScope HTTP API 发 5 并发 → 全部 @0s 开始,总 3.4s ≈ 单次。
   → DashScope 服务端并发没问题,不是 API key 限制。

3. **排除 SDK 串行**: dashscope SDK 用 5 个原生线程并发调用 → 总 5.6s ≈ 单次。
   → SDK 本身支持并发(网络 I/O 时释放 GIL)。

4. **发现进程陷阱**: 第一次改 `def` 后重测**仍然串行**。检查发现
   `pkill -f "flask_service/app.py"` 没杀掉旧进程(进程命令行是 `python app.py`,
   不含 "flask_service/" 路径),新进程因 **"address already in use" 静默退出**。
   → 之前所有测试打的都是旧的 `async def` 进程,修复从未生效。

5. **按端口精确杀进程** (`lsof -iTCP:8001 -t`) 重启后:
   5 并发隔离测试 44.8s → 8.7s,完整 benchmark 10 并发吞吐 0.20 → 1.06 r/s。
   → 修复验证成功。

**教训**: `pkill -f <pattern>` 匹配的是完整命令行;在子目录里 `cd` 后启动的
`python app.py`,命令行不含目录路径。重启服务应按端口杀进程,并检查启动日志
确认没有 "address already in use"。

---

## 四、代码修改

文件: `flask_service/app.py`

```python
# 1. 新增导入
import asyncio

# 2. /rag/query (无内部 await,直接转同步)
- async def rag_query(req: RagRequest):
+ def rag_query(req: RagRequest):  # sync def → FastAPI threadpool, 不阻塞事件循环

# 3. /api/predict/analyze (同上)
- async def predict_analyze(req: PredictRequest):
+ def predict_analyze(req: PredictRequest):  # sync def → threadpool

# 4. /ocr/predict (有 await file.read(), 保留 async, 阻塞调用入线程池)
  async def ocr_predict(file: UploadFile = File(...)):
      img_bytes = await file.read()
-     resp = MultiModalConversation.call(model=OCR_MODEL, messages=[...])
+     resp = await asyncio.to_thread(
+         MultiModalConversation.call, model=OCR_MODEL, messages=[...])
```

---

## 五、对整体架构的意义

1. **解释了为什么 Spring Boot 缓存层这么关键**:
   缓存 miss 时单次 RAG ~5-6s(走 LLM),且修复前并发还会排队恶化。
   Spring Boot 的 Redis 缓存命中直接返回(~5ms),绕过整个 Flask+LLM 链路。
   这正是之前 benchmark 中 RAG 端点 avg 5.8ms(100% 缓存命中)的原因。

2. **解释了为什么压测用 mock 模式**:
   `agent.mock.enabled=true` 拦截 LLM 出站,正是为了绕过这个 ~5s 的 LLM 瓶颈,
   测前序链路(nginx/Spring/DB)的真实吞吐。本次修复让"非 mock"路径的并发能力
   也提升了 5×。

3. **生产部署建议**:
   - Flask 单实例并发能力: 修复后 ~1 req/s/(每 6s LLM) × 线程池 40 ≈ 受 LLM 延迟约束
   - 进一步提升可加 uvicorn workers (`--workers N`) 或 gunicorn
   - 缓存命中率仍是 RAG 性能的第一杠杆(Spring Boot 层已实现)

---

## 六、复现命令

```bash
# 1. 启动 Flask (宿主机, 按设计 Flask 不在 Docker)
cd flask_service
export DASHSCOPE_API_KEY=sk-... NEO4J_ENABLED=false REDIS_HOST=localhost
/opt/miniconda3/envs/zjy/bin/python app.py &

# 2. 跑并发 benchmark
cd test/perf
/opt/miniconda3/envs/zjy/bin/python flask_rag_direct_benchmark.py --n 15

# 3. 重启服务时务必按端口杀进程(不要用 pkill -f 路径)
kill -9 $(lsof -nP -iTCP:8001 -sTCP:LISTEN -t)
```

---

## 七、遗留 / 下一步

- [ ] **全栈验证**: 起 Docker(mysql/redis/agent-service/nginx)+ 宿主机 Flask,
      测 Spring Boot 缓存层 hit/miss 端到端(本次只测了 Flask 直调层)
- [ ] **uvicorn workers**: 评估多 worker 是否进一步提升(当前单 worker + 40 线程池)
- [ ] **OCR 并发实测**: 本次只验证了 RAG/predict,OCR 改动已就位但未压测
- [ ] 把 `def` 修复同步到生产 Flask 镜像 / Dockerfile

---

**测试脚本**: `test/perf/flask_rag_direct_benchmark.py`
**结果数据**:
- 修复前: `test/perf/flask_rag_direct_1780047366.json`
- 修复后: `test/perf/flask_rag_direct_1780058726.json`
