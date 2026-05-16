# AI / RAG Benchmark 测试指令

## 目标

用于评测 MedicalAssistant 的 AI 相关能力：

- RAG 问答准确率、错误率、响应速度。
- Agent 聊天链路准确率、错误率、响应速度。
- Predict 不良反应预测链路可用性、错误率、响应速度。

本指令不依赖本机一定能访问模型服务。评测脚本和样例数据已经放在 `docs` 目录，拿到能访问 Spring Boot、Flask、DashScope 的机器后即可执行。

## 代码入口

后端免认证评测接口：

- `POST /api/rag/query`
  - 请求体：`{"question":"...","with_trace":true,"with_timing":true}`
  - 响应核心字段：`success`、`answer`、`error`、`timings`、`trace`
- `POST /api/agent/chat`
  - 请求体：`{"user_id":"10001","session_id":"bench-001","message":"...","with_trace":true,"with_timing":true}`
  - 响应为统一 `Result` 包装，核心内容在 `data.assistant_message`
- `POST /api/predict/analyze`
  - 请求体：`{"text":"Clinical Summary: ..."}`
  - 响应为统一 `Result` 包装，核心内容在 `data.status`、`data.predictions`

当前相关实现位置：

- `src/main/java/com/whu/medicalbackend/agent/controller/AgentProxyController.java`
- `src/main/java/com/whu/medicalbackend/agent/rag/RagController.java`
- `src/main/java/com/whu/medicalbackend/agent/predict/PredictController.java`
- `src/main/java/com/whu/medicalbackend/agent/rag/RagRequest.java`
- `src/main/java/com/whu/medicalbackend/agent/rag/RagResponse.java`
- `src/main/java/com/whu/medicalbackend/agent/predict/PredictRequest.java`
- `src/main/java/com/whu/medicalbackend/agent/predict/PredictResponse.java`

## 运行前置

1. Spring Boot 已启动。
2. Flask AI 服务已启动。
3. 如果要测 Agent 大模型能力，必须配置 `DASHSCOPE_API_KEY`。
4. 如果 Spring Boot 在本机直接运行，默认访问 `FLASK_URL=http://127.0.0.1:8001`。
5. 如果 Spring Boot 在 Docker 中运行且 Flask 在宿主机，`docker-compose*.yml` 使用 `FLASK_URL=http://host.docker.internal:8001`。

健康检查：

```bash
curl -s http://127.0.0.1:8080/api/agent/health
curl -s -X POST http://127.0.0.1:8080/api/rag/query \
  -H 'Content-Type: application/json' \
  -d '{"question":"布洛芬有哪些常见不良反应？","with_trace":true,"with_timing":true}'
```

## 快速执行

使用样例评测集：

```bash
cd /Users/mac/Desktop/project/MedicalAssistant
python3 test/ai-rag-benchmark/ai_rag_benchmark.py \
  --base-url http://127.0.0.1:8080 \
  --cases test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl \
  --concurrency 2 \
  --repeat 1 \
  --warmup 1
```

并发压测：

```bash
python3 test/ai-rag-benchmark/ai_rag_benchmark.py \
  --base-url http://127.0.0.1:8080 \
  --cases test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl \
  --concurrency 8 \
  --repeat 5 \
  --timeout 180
```

脚本输出：

- 控制台：每条用例 PASS / FAIL 和耗时。
- `test/ai-rag-benchmark/benchmark-results/*.json`：完整 summary 与逐条明细。
- `test/ai-rag-benchmark/benchmark-results/*.csv`：可直接导入表格分析。

## 用例格式

评测集使用 JSONL，一行一个用例。推荐复制 `test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl` 另存为正式文件，例如：

```bash
cp test/ai-rag-benchmark/ai_rag_benchmark_cases.sample.jsonl test/ai-rag-benchmark/ai_rag_benchmark_cases.formal.jsonl
```

RAG 用例：

```json
{"id":"rag-001","endpoint":"rag","question":"布洛芬有哪些常见不良反应？","expected_keywords":["布洛芬","胃肠道"],"forbidden_keywords":["无法回答"]}
```

Agent 用例：

```json
{"id":"agent-001","endpoint":"agent","user_id":"10001","session_id":"bench-agent-001","message":"请查询医学知识库：阿司匹林有哪些常见不良反应？","expected_keywords":["阿司匹林"],"forbidden_keywords":["LLM 未配置"]}
```

Predict 用例：

```json
{"id":"predict-001","endpoint":"predict","text":"Clinical Summary: 68-year-old patient with hypertension. Current medication includes ibuprofen.","expected_status":"success","expected_reactions":["nausea"],"top_k":5}
```

字段说明：

- `id`：用例唯一标识。
- `endpoint`：`rag`、`agent`、`predict`。
- `question` / `message` / `text`：分别用于 RAG、Agent、Predict。
- `payload`：可选，直接覆盖请求体，适合特殊字段测试。
- `expected_keywords`：答案中必须包含的关键词，用于粗粒度准确率。
- `forbidden_keywords`：答案中不能出现的关键词。
- `expected_reactions`：Predict 前 `top_k` 个预测中必须出现的反应名称。
- `min_top_probability`：Predict 第一条结果的最低概率阈值。
- `expected_status`：Predict 默认期望 `success`。

## 指标定义

脚本内置指标：

- `accuracy`：通过用例数 / 总用例数。
- `error_rate`：HTTP 异常、非 2xx、统一 Result code 非预期、脚本异常的比例。
- `avg_ms`：平均响应时间。
- `p50_ms`、`p90_ms`、`p95_ms`、`p99_ms`：响应时间分位数。
- `throughput_rps`：总请求数 / 总耗时。
- `by_endpoint`：按 RAG、Agent、Predict 分组统计。

准确率判定是可解释的规则评分，不是语义相似度评分。正式验收建议使用人工标注的 gold set，把每个问题的关键医学事实写入 `expected_keywords`。

## 推荐正式评测集规模

- Smoke：每类 3 到 5 条，用于发布前快速确认链路可用。
- Regression：每类 30 到 50 条，用于每次后端或模型改动后回归。
- Benchmark：RAG 100 条以上，Agent 50 条以上，Predict 50 条以上，并记录固定版本号、数据集版本和服务配置。

建议覆盖类别：

- RAG：药品不良反应、用药禁忌、剂量注意事项、慢病管理、症状处理边界。
- Agent：普通健康问答、明确要求查询知识库、要求调用 Predict、含上下文的多轮问题。
- Predict：不同年龄、病史、药物组合、低风险样本、高风险样本。

## 结果解读模板

每次正式评测后记录：

```text
评测时间：
代码版本：
数据集文件：
Spring Boot 地址：
Flask 地址：
DashScope 模型：
并发数：
重复次数：

总体 accuracy：
总体 error_rate：
总体 p95：
RAG accuracy / p95：
Agent accuracy / p95：
Predict accuracy / p95：

主要失败样例：
性能瓶颈判断：
下一步修复项：
```

## 注意事项

- `DASHSCOPE_API_KEY` 缺失时 Agent 服务不会崩溃，但 Agent 问答会返回 LLM 未配置提示，此时 Agent benchmark 会失败，这是预期行为。
- RAG 和 Predict 最终依赖 Flask 服务。Spring Boot 正常但 Flask 不可用时，错误率会上升。
- Docker 内部不能用 `127.0.0.1` 访问宿主机 Flask，需要使用 `host.docker.internal`。
- Benchmark 时不要混用不同模型版本和不同知识库版本，否则准确率不可比。
