# Flask AI 服务并发优化与压测分析报告

> 项目：MedicalAssistant Flask AI 服务（RAG / Predict / OCR）  
> 日期：2026-05-31  
> 模型：qwen-turbo（DashScope）  
> 服务：medical-fastapi-ai（FastAPI + uvicorn, 端口 8001）

---

## 目录

1. [核心功能：Flask AI 服务在宿主机侧的架构与改动](#1-核心功能flask-ai-服务在宿主机侧的架构与改动)
2. [问题分析：并发、性能、可靠性挑战](#2-问题分析并发性能可靠性挑战)
3. [技术方案：针对每个问题的解决方案](#3-技术方案针对每个问题的解决方案)
4. [测试评估：测试方案与效果](#4-测试评估测试方案与效果)

---

## 1. 核心功能：Flask AI 服务在宿主机侧的架构与改动

### 1.1 整体架构概览

Flask AI 服务（实为 FastAPI）是整个系统的 AI 能力提供方，**按设计运行在宿主机上**（不在 Docker 内）。两份 compose 文件（`docker-compose.yml` / `docker-compose.microservices.yml`）均通过 `FLASK_URL: http://host.docker.internal:8001` 让容器内的 Spring Boot 服务访问宿主机 Flask。架构分层如下：

```
Spring Boot (Docker 容器, agent-service)
  └── UnifiedFlaskClient (host.docker.internal:8001)
       └── FastAPI / uvicorn (宿主机, 单 worker)
            ├── POST /rag/query        — RAG 检索增强问答
            │    ├── _build_graph_context (Neo4j 图谱增强, 可选)
            │    └── call_llm → DashScope Generation.call (qwen-turbo)
            ├── POST /api/predict/analyze — 药物不良反应预测
            │    ├── _predict_cache_get (Redis 缓存)
            │    └── call_llm → DashScope Generation.call
            ├── POST /ocr/predict      — 药品说明书 OCR
            │    └── MultiModalConversation.call (qwen-vl)
            └── GET  /health           — 健康探活
```

### 1.2 核心改动清单

| 模块 | 改动 | 文件 |
|------|------|------|
| RAG 端点并发修复 | `async def` → `def`，避免阻塞 SDK 卡死事件循环 | `flask_service/app.py` |
| Predict 端点并发修复 | `async def` → `def`，同上 | `flask_service/app.py` |
| OCR 端点并发修复 | 保留 `async`，阻塞 SDK 调用包入 `asyncio.to_thread` | `flask_service/app.py` |
| 并发 benchmark 脚本 | 串行/5并发/10并发三轮对比，测真实 LLM 延迟分布 | `test/perf/flask_rag_direct_benchmark.py` |

### 1.3 RAG 请求执行路径

`/rag/query` 的执行路径：

```
rag_query() 入口 (修复后: def, 由 FastAPI 线程池调度)
  ├── question 校验 (空问题 → 400)
  ├── _build_graph_context() — Neo4j 图谱增强 (NEO4J_ENABLED 控制)
  │    └── 命中药物实体 → 注入结构化药物不良事件数据到 prompt
  ├── call_llm() — DashScope qwen-turbo 同步阻塞调用 (~5-6s)
  └── 组装响应 (answer / sources / cache_hit / elapsed_ms / timings)
```

> 关键点：Flask 端 RAG **不做结果缓存**（`cache_hit` 恒为 `false`），缓存由 Spring Boot 层（`RagCacheService` + Redis）承担。Flask 直调测的就是「缓存 miss / LLM 调用」的真实成本。

### 1.4 三个端点的阻塞调用模式

| 端点 | 修复前签名 | 阻塞调用 | 是否有 await |
|------|-----------|---------|------------|
| `/rag/query` | `async def` | `Generation.call` | 无 |
| `/api/predict/analyze` | `async def` | `Generation.call` | 无 |
| `/ocr/predict` | `async def` | `MultiModalConversation.call` | 有 `await file.read()` |

### 1.5 call_llm 同步调用实现

```python
# flask_service/app.py
def call_llm(messages, model=RAG_MODEL, max_tokens=1024) -> str:
    resp = Generation.call(            # ← DashScope SDK 同步阻塞调用
        model=model,
        messages=messages,
        max_tokens=max_tokens,
        result_format="message",
        temperature=0.1,
        timeout=28,
    )
    if resp.status_code != 200:
        raise RuntimeError("DashScope error %s: %s" % (resp.status_code, resp.message))
    return resp.output.choices[0].message.content.strip()
```

### 1.6 运行环境参数

| 参数 | 值 | 说明 |
|------|-----|------|
| 运行环境 | zjy conda (Python 3.11) | 宿主机虚拟环境 |
| uvicorn worker | 1 | 单 worker（默认） |
| NEO4J_ENABLED | false | 测试时关闭图谱增强 |
| REDIS_HOST | localhost | Predict 缓存 + Flask 侧 Redis |
| DASHSCOPE_API_KEY | sk-e705…b15 | qwen-turbo，实测可用 |

---

## 2. 问题分析：并发、性能、可靠性挑战

### 2.1 性能挑战

#### P1: 并发请求被迫串行 — 吞吐卡死 0.2 req/s
- 三个 AI 端点用 `async def` 声明，但内部调用**同步阻塞**的 dashscope SDK（`Generation.call` / `MultiModalConversation.call`）
- 在单 worker uvicorn 下，阻塞调用直接卡死 asyncio 事件循环
- 实测：并发 1/5/10 三档，吞吐均为 0.2 req/s 完全不变，总耗时恒为 ~70s
- 影响：并发越高延迟越爆炸（4.4s → 35s），但吞吐毫无提升

#### P2: 缓存 miss 时单次 RAG 延迟高 — 5-6s
- 缓存未命中需走完整 LLM 链路，单次 RAG ~5-6s（qwen-turbo 生成）
- 瓶颈点：LLM 推理 + 网络传输（图谱增强额外开销）
- 影响：用户首次提问体验差，无缓存兜底时延迟敏感

### 2.2 可靠性挑战

#### R1: 重启服务时进程残留导致改动静默失效
- `pkill -f "flask_service/app.py"` 无法杀掉进程（进程命令行实为 `python app.py`，不含 "flask_service/" 路径）
- 旧进程占用 8001，新进程因 `address already in use` 静默退出
- 影响：代码改动看似无效，误导排查方向

### 2.3 可观测性挑战

#### O1: 瓶颈定位困难 — 多因素互相掩盖
- 串行现象可能由「账号限速 / SDK 内部锁 / 事件循环阻塞 / 进程残留」任一造成
- 缺乏分层隔离测试，难以快速定位真因

---

## 3. 技术方案：针对每个问题的解决方案

### 3.1 性能优化方案

#### 针对 P1: 并发串行

**方案：同步处理器 + 线程池调度**

FastAPI 对 `def`（非 `async def`）路由处理器会自动用 anyio 线程池（默认 40 线程）调度，使阻塞调用在独立线程中并发执行（网络 I/O 时释放 GIL）。

```python
# 1. 新增导入
import asyncio

# 2. /rag/query (无内部 await, 直接转同步 def)
- async def rag_query(req: RagRequest):
+ def rag_query(req: RagRequest):  # sync def → FastAPI 线程池, 不阻塞事件循环

# 3. /api/predict/analyze (同上)
- async def predict_analyze(req: PredictRequest):
+ def predict_analyze(req: PredictRequest):  # sync def → 线程池

# 4. /ocr/predict (有 await file.read(), 保留 async, 阻塞调用入线程池)
  async def ocr_predict(file: UploadFile = File(...)):
      img_bytes = await file.read()
-     resp = MultiModalConversation.call(model=OCR_MODEL, messages=[...])
+     resp = await asyncio.to_thread(
+         MultiModalConversation.call, model=OCR_MODEL, messages=[...])
```

**为何有效**（分层验证排除其他因素）：

```
1. 直连 DashScope HTTP API, 5 并发 → 总 3.4s ≈ 单次  → 排除账号限速
2. dashscope SDK 原生 5 线程并发    → 总 5.6s ≈ 单次  → 排除 SDK 内部串行
3. ∴ 瓶颈 = async def 处理器内阻塞调用卡死事件循环
4. 改 def 后由线程池调度 → 并发恢复
```

#### 针对 P2: 缓存 miss 延迟高

**方案：依赖 Spring Boot 层 Redis 缓存兜底**

Flask 端 RAG 不缓存（设计如此），缓存命中由 Spring Boot 的 `RagCacheService` 承担。缓存命中直接返回 ~5ms，绕过整条 Flask + LLM 链路。本次并发修复进一步保证：缓存 miss 集中到来时，Flask 能并发处理而非排队恶化。

### 3.2 可靠性优化方案

#### 针对 R1: 进程残留

**方案：按端口精确杀进程 + 检查启动日志**

```bash
# 错误方式 (杀不掉, 进程命令行不含路径)
pkill -f "flask_service/app.py"

# 正确方式 (按监听端口定位 PID)
kill -9 $(lsof -nP -iTCP:8001 -sTCP:LISTEN -t)

# 重启后必须检查启动日志确认无 "address already in use"
tail -4 /tmp/flask_service.log
```

### 3.3 可观测性优化方案

#### 针对 O1: 瓶颈定位

**方案：分层隔离压测脚本**

`test/perf/flask_rag_direct_benchmark.py` 对 Flask 直接施压，与 Spring Boot 缓存层解耦，专测「缓存 miss / LLM 调用」路径。配合直连 DashScope、SDK 多线程两个对照实验，三层定位法快速排除干扰因素。

---

## 4. 测试评估：测试方案与效果

### 4.1 测试场景设计

针对 RAG 端点（`/rag/query`），用真实医疗问题施压，对比修复前后三个并发档位。

| 轮次 | 并发 | 题数 | 测试目标 |
|------|------|------|---------|
| Round 1 | 1（串行） | 15 | LLM 单次延迟基线 |
| Round 2 | 5 | 15 | 中并发吞吐 |
| Round 3 | 10 | 15 | 高并发吞吐 |

### 4.2 测试环境

| 项目 | 值 |
|------|-----|
| 部署方式 | 宿主机直跑（zjy conda, 单 uvicorn worker） |
| 模型 | qwen-turbo（DashScope，真实调用） |
| Neo4j | 关闭（NEO4J_ENABLED=false） |
| 题库 | `test/medical_questions_100.txt`（取前 15 题） |
| 工具 | `flask_rag_direct_benchmark.py`（aiohttp + Semaphore） |

### 4.3 修复前结果（`async def`, 阻塞事件循环）

| 并发 | 总耗时 | 吞吐 | P50 延迟 | P95 延迟 | 单次 LLM | 成功率 |
|------|--------|------|---------|---------|---------|-------|
| 1 | 69.9s | 0.21 r/s | 4.4s | 8.5s | 4.66s | 100% |
| 5 | 71.7s | 0.21 r/s | 22.7s | 28.3s | 4.78s | 100% |
| 10 | 73.9s | 0.20 r/s | 35.4s | 54.8s | 4.93s | 100% |

**特征**：并发上升，吞吐不变（卡死 0.2 r/s），延迟线性暴涨 → 典型串行排队。单次 LLM 稳定 ~4.7s，但总延迟远大于它 → 请求在事件循环后排队。

### 4.4 修复后结果（`def`, 线程池）

| 并发 | 总耗时 | 吞吐 | P50 延迟 | P95 延迟 | 单次 LLM | 成功率 |
|------|--------|------|---------|---------|---------|-------|
| 1 | 88.8s | 0.17 r/s | 5.9s | 8.0s | 5.92s | 100% |
| 5 | 22.1s | **0.68 r/s** | **6.3s** | 8.4s | 6.13s | 100% |
| 10 | 14.2s | **1.06 r/s** | **6.3s** | 9.4s | 6.23s | 100% |

**特征**：P50 延迟稳定 ~6.3s 不随并发变化（无排队），吞吐随并发线性增长。

> 注：修复后 Round 1 串行略慢（88.8s vs 69.9s）是 DashScope 当次 LLM 延迟波动（单次 5.9s vs 4.7s），与修复无关。串行吞吐天然受单次 LLM 时间限制，修复只影响并发路径。

### 4.5 关键对比分析

#### 修复前 vs 修复后（10 并发）

| 指标 | 修复前 | 修复后 | 提升 |
|------|--------|--------|------|
| 吞吐 | 0.20 r/s | 1.06 r/s | **5.3×** |
| P50 延迟 | 35.4s | 6.3s | **5.6×** |
| P95 延迟 | 54.8s | 9.4s | **5.8×** |

#### 5 并发隔离测试（单独验证）

| 测试 | 总墙钟 | 说明 |
|------|--------|------|
| 修复前（async def） | 44.8s | 完成时间 9/26/32/39/44s，逐个串行 |
| 修复后（def） | 8.7s | 全部 @0s 开始，5-8.7s 内重叠完成 |

#### Predict 端点并发验证

| 测试 | 总墙钟 | 说明 |
|------|--------|------|
| 修复后 5 并发 | 5.19s | ≈ 单次耗时，并发正常 |

### 4.6 问题复现与验证

| 问题 | 是否复现 | 分析 |
|------|---------|------|
| 并发串行（吞吐卡死） | 复现 → 已修复 | async def + 阻塞 SDK，改 def 后吞吐 5.3× |
| DashScope 账号限速 | 未复现 | 直连 5 并发 3.4s ≈ 单次，服务端并发正常 |
| SDK 内部串行 | 未复现 | 原生多线程 5.6s ≈ 单次，SDK 支持并发 |
| 进程残留致改动失效 | 复现 | pkill 未杀掉旧进程，新进程端口冲突静默退出 |

### 4.7 性能基准数据

| 指标 | 修复前 | 修复后 |
|------|--------|--------|
| RAG 单次 LLM 延迟（串行） | 4.66s | 5.92s（API 波动） |
| RAG 吞吐（5 并发） | 0.21 r/s | 0.68 r/s |
| RAG 吞吐（10 并发） | 0.20 r/s | 1.06 r/s |
| RAG P50 延迟（10 并发） | 35.4s | 6.3s |
| Predict 端点并发 | 串行 | 正常（5 并发 5.19s） |
| 成功率（全场景） | 100% | 100% |

### 4.8 为何此前的 benchmark 未发现该 bug

项目原有 `test/ai-rag-benchmark/ai_rag_benchmark.py`（同为本人编写）用 `ThreadPoolExecutor` 并发施压，但它与本次脚本测的**不是同一层**：

| 维度 | 原 ai_rag_benchmark.py | 本次 flask_rag_direct_benchmark.py |
|------|----------------------|-----------------------------------|
| 目标端点 | `/api/rag/query`（**Spring Boot**，经缓存层） | `/rag/query`（**Flask 直调**，无缓存） |
| 并发模型 | ThreadPoolExecutor | aiohttp + Semaphore |
| 用例特征 | `--repeat` 重复相同问题 | 15 个**不同**问题 |
| 实测缓存命中率 | RAG 端点 **100%** | 0%（Flask 不缓存） |
| RAG avg | **5.838ms** | 4661ms（修复前）/ 6126ms（修复后） |

**bug 被掩盖的原因**：原 benchmark 经过 Spring Boot Redis 缓存层 + 重复相同问题，第一次调用后即缓存命中（5.8ms 直接返回），**并发请求从未真正并发打到 Flask**。该并发串行 bug 只在「大量**不同的** cache-miss 问题同时到达 Flask」时才暴露 —— 这正是本次直调脚本构造的场景。

**启示**：缓存层会掩盖下游 provider 的并发缺陷。压测应分层进行：既要测「带缓存」的端到端吞吐（反映生产典型负载），也要测「直调 provider」的 cache-miss 路径（反映缓存击穿 / 冷启动 / 缓存雪崩时的真实承压能力）。

### 4.9 对整体架构的意义

1. **解释了 Spring Boot 缓存层为何关键**：缓存 miss 时单次 RAG ~5-6s 走 LLM，修复前并发还会排队恶化。Spring Boot 的 Redis 缓存命中直接返回 ~5ms，绕过整条 Flask + LLM 链路 —— 这正是之前 benchmark 中 RAG 端点 avg 5.8ms（100% 缓存命中）的原因。
2. **解释了压测为何用 mock 模式**：`agent.mock.enabled=true` 拦截 LLM 出站，正是为绕过这个 ~5s 的 LLM 瓶颈，测前序链路真实吞吐。本次修复让「非 mock」路径的并发能力也提升 5×。

### 4.10 遗留问题

1. **全栈端到端未测**：本次只测 Flask 直调层（缓存 miss 路径）。起 Docker（mysql/redis/agent-service/nginx）+ 宿主机 Flask 的全栈 hit/miss 对比待补。
2. **uvicorn 多 worker 未评估**：当前单 worker + 40 线程池，受 LLM 延迟约束（~6s/请求）。`--workers N` 或 gunicorn 是否进一步提升待测。
3. **OCR 端点未压测**：改动已就位（`asyncio.to_thread`），但只验证了 RAG/Predict 的并发，OCR 并发实测待补。
4. **修复未上线**：`flask_service/app.py` 源码已改，但未提交 git、未同步生产 Docker 镜像。

---

## 附录 A: 测试环境配置

### Flask 启动命令

```bash
cd flask_service
export DASHSCOPE_API_KEY=sk-e7051a4ddaa049e9bd25c8264dfb3b15
export NEO4J_ENABLED=false
export REDIS_HOST=localhost
export REDIS_PORT=6379
/opt/miniconda3/envs/zjy/bin/python app.py &
```

### benchmark 运行命令

```bash
cd test/perf
/opt/miniconda3/envs/zjy/bin/python flask_rag_direct_benchmark.py --n 15

# 重启 Flask 务必按端口杀进程 (不要用 pkill -f 路径)
kill -9 $(lsof -nP -iTCP:8001 -sTCP:LISTEN -t)
```

## 附录 B: 三层定位法（瓶颈排查逻辑链）

```
现象: Flask /rag/query 并发 1/5/10 吞吐均 0.2 r/s, 不提速
  │
  ├─ 实验1: 直连 DashScope HTTP, 5 并发 → 3.4s ≈ 单次
  │         ∴ 排除账号限速
  │
  ├─ 实验2: dashscope SDK 原生 5 线程 → 5.6s ≈ 单次
  │         ∴ 排除 SDK 内部串行
  │
  ├─ 实验3: 改 def 后重测 → 仍串行 (44.8s)
  │         发现 pkill 未杀掉旧进程, 改动从未生效
  │
  └─ 修正: 按端口杀进程重启 → 5 并发 8.7s, 10 并发吞吐 1.06 r/s
            ∴ 根因 = async def 内阻塞调用卡死事件循环
```

## 附录 C: 核心文件清单

| 文件路径 | 功能 |
|---------|------|
| `flask_service/app.py` | FastAPI AI 服务（RAG/Predict/OCR 端点） |
| `flask_service/app.py::call_llm` | DashScope Generation.call 同步封装 |
| `test/perf/flask_rag_direct_benchmark.py` | RAG 直调并发压测脚本（串行/5/10 三档，本次新增） |
| `test/ai-rag-benchmark/ai_rag_benchmark.py` | 原 Spring Boot 端到端 benchmark（经缓存层） |
| `docs/rag-flask-concurrency-fix-VERIFIED.md` | 实测修复详细记录 |

### Flask 服务提交历史（均由 creeper-RedWHU / zhoujinyao 编写）

| commit | 内容 | app.py 改动 |
|--------|------|-----------|
| `ec7ba1e` | Flask→FastAPI、SSE 流式（Spring 侧）、RAG/Agent benchmark | +220 行（建服务） |
| `bf709bf` | Neo4j 图谱增强 + Predict 缓存 + 模型统一 qwen-turbo | +279 行 |
| `27f348e` | 降低 Flask 读超时 120s→30s + DashScope SDK timeout | +1 行 |
| （本次未提交） | 三端点并发修复（async def→def / asyncio.to_thread） | 3 处 |

## 附录 D: 结果数据文件

| 文件 | 内容 |
|------|------|
| `test/perf/flask_rag_direct_1780047366.json` | 修复前基线数据 |
| `test/perf/flask_rag_direct_1780058726.json` | 修复后验证数据 |
