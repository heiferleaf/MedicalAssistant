# AI 服务优化贡献记录

> 作者：creeper-RedWHU
> 涉及提交：`ec7ba1e`, `bf709bf`

---

## 一、Flask → FastAPI 迁移

**背景**：原 AI 服务基于同步 Flask，无法利用异步 IO，高并发下线程资源浪费严重。

**改动**：
- 将 `flask_service/app.py` 完整迁移至 FastAPI + Pydantic v2，所有路由改为 `async def`
- 启动方式从 `flask run` 改为 `uvicorn app:app --workers 4`（4 进程），充分利用多核
- `Dockerfile` 更新为多进程 uvicorn 启动命令
- 三个端点均用 Pydantic BaseModel 做请求校验，非法请求自动返回 422（原 Flask 无校验）

**效果**：异步 IO 下并发吞吐量显著提升，`/docs` 自动生成 OpenAPI 文档。

---

## 二、消除假流式 SSE，改用真实 Token 流

**背景**：`AgentOrchestratorService.chatStream()` 原实现是调用阻塞式 `chatModel.chat()` 拿到完整回答，再用 `Thread.sleep(50)` 循环逐字切割假装流式，增加了不必要延迟，且用户体验差（等待全量响应后才开始收到内容）。

**改动**（`AgentOrchestratorService.java`）：

- **MedicalAgent 路径**：移除 `Thread.sleep(50)` 的分割循环，改为拿到完整 `assistantMessage` 后直接 `emitter.send()` 一次，再发 `end` 事件
- **简单 LLM 路径**：引入 `QwenStreamingChatModel`（`dev.langchain4j.community.model.dashscope`），用 `StreamingChatResponseHandler` + `CountDownLatch` 实现真实 Token 流：
  - `onPartialResponse(token)` → 每个 token 即时 `emitter.send()`
  - `onCompleteResponse()` / `onError()` → `latch.countDown()` 解除阻塞
  - SSE 线程通过 `latch.await()` 等待流结束后发 `end` 事件

**LangChain4jConfig.java**：新增 `StreamingChatModel` Bean（`@ConditionalOnProperty` + `@ConditionalOnExpression` 双重条件保护，API Key 空时不注册）。

**效果**：首 token 延迟从"等待全量响应"缩短至"模型输出第一个 token 即推送"，用户感知响应速度大幅改善。

---

## 三、Neo4j 药物知识图谱 RAG 增强

**背景**：项目中已部署 `neo4j-medical` 容器（FAERS 真实世界药物不良事件数据库，约 175k Drug / 186k Reaction 节点），但完全未接入 RAG 链路，是闲置资源。

**图谱结构**（通过探索确认）：
```
(DrugSet)-[:CONTAINS_DRUG]->(Drug {drugname})
(DrugSet)-[:CAUSES_REACTION]->(Reaction {reac})
(DrugSet)-[:TREATS_FOR]->(Indication {indi_pt})
```

**改动**（`flask_service/app.py`）：

- 新增 `DRUG_NAME_MAP`：中文药名→Neo4j 英文标准名映射（20种常用药）
- `_extract_drug_names(question)`：从问题中提取涉及药物（中英文均支持）
- `_query_drug_reactions(drug_names)`：查询 Top-8 已报告不良反应
- `_query_drug_indications(drug_names)`：查询 Top-5 适应症
- `_build_graph_context(question)`：组装结构化知识文本，注入 LLM Prompt 前缀

**注入格式示例**：
```
【来自药物不良事件数据库（FAERS）的结构化知识 — 查询耗时 182ms】
涉及药物：IBUPROFEN
已报告不良反应（Top 8）：Pain, Fatigue, Headache, Nausea, Diarrhoea, ...
（以上数据来源于真实世界药物警戒报告，供参考）
```

响应新增字段：`neo4j_augmented: true/false`、`timings.neo4j_ms`。

**效果**：含药物名称的 RAG 问题，LLM 回答中会明确引用 FAERS 数据，信息更具体（实测布洛芬、阿司匹林+华法林联用问题均触发增强）。Neo4j 查询约 170ms，属于可接受开销。

---

## 四、Predict 接口 Redis 缓存

**背景**：`/api/predict/analyze` 每次调用都打 DashScope（约 3s），相同临床摘要会重复计算。

**改动**：
- 缓存 Key：`ai:predict:v1:{SHA-256(input_text)}`
- TTL：1800s + ±10% 随机抖动（防止大量缓存同时失效引发 stampede）
- Redis 连接失败时自动降级（`_get_redis()` 返回 None，跳过缓存逻辑不报错）
- 响应新增 `cache_hit: true/false` 字段

**效果**：

| 场景 | 耗时 |
|------|------|
| 首次调用（冷） | ~3s |
| 缓存命中（热） | ~10ms |
| 加速比 | **300x** |

---

## 五、模型统一：qwen-plus → qwen-turbo

**背景**：Flask `app.py` 使用 `qwen-plus`，Spring `application.yaml` 配置的是 `qwen-turbo`，两者行为不一致，且 `qwen-plus` 延迟更高。

**改动**：`RAG_MODEL = "qwen-turbo"`，与 Spring 侧保持一致。

**效果**：RAG 冷请求延迟从约 18s 降至约 5s（同模型，不同 top-k 等参数有浮动）。

---

## 六、性能测试脚本

新增 `test/perf/perf_test.py`（纯标准库，无第三方依赖）：

| 测试项 | 说明 |
|--------|------|
| 健康检查 | Spring + Flask 双端 |
| RAG 冷/热延迟 | 5条问题，统计 avg/p50/p90/p99 |
| Spring 代理 vs Flask 直调 | 量化代理层开销 |
| Predict 延迟 | Spring vs Flask |
| Agent 聊天延迟 | SSE 流完成时间 |
| 并发压测 | 1/4/8/16 并发梯度 |
| Neo4j 直查延迟 | 药物-不良反应图查询基准 |

用法：
```bash
python3 test/perf/perf_test.py --base-url http://127.0.0.1:80 --flask-url http://127.0.0.1:8001
python3 test/perf/perf_test.py --skip-neo4j --skip-agent   # 跳过慢测试
```

---

## 综合性能收益

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| RAG 冷请求延迟 | ~18s (qwen-plus) | ~5s (qwen-turbo) |
| RAG 热请求延迟 | <5ms（Spring Redis 缓存，已有） | <5ms |
| Predict 热请求延迟 | ~3s（无缓存） | ~10ms（Redis 缓存） |
| SSE 首 token 时间 | 等待全量响应后才推送 | 模型输出第一个 token 即推送 |
| 药物问题回答质量 | 纯 LLM 知识 | LLM + FAERS 真实世界数据增强 |
