# 快速验证与运维指南

> 日常开发、测试、部署时快速检查系统状态和性能

---

## 🟢 秒级健康检查

```bash
# 1. 应用健康状态
curl -s http://localhost:8080/health | jq '.status'
# 期望: UP

# 2. 核心指标（Actuator）
curl -s http://localhost:8080/actuator/metrics | jq '.names[]' | grep -E "http|process|jvm"

# 3. 快速功能测试
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "test_user",
    "session_id": "test_session_'$(date +%s)'",
    "message": "你好"
  }' | jq '.success'
# 期望: true
```

---

## 📊 配置验证

### 检查关键参数是否加载

```bash
# aiExecutor 线程池配置
grep -A 3 "ai:" src/main/resources/application.yaml | grep "core-size"
# 期望: core-size: 20 (或优化后的值)

# Sentinel 限流配置
grep "sentinel:" src/main/resources/application.yaml -A 5
# 期望:
#   chat-qps: 500
#   stream-qps: 300
#   ocr-qps: 30

# RAG 缓存配置
grep "rag:" src/main/resources/application.yaml -A 10 | grep -E "ttl|enabled"
# 期望: enabled: true, ttl-seconds: 1800

# nginx keepalive 配置
grep "keepalive" nginx/nginx.microservices.conf
# 期望: keepalive 64;
```

### 通过 REST 端点验证配置

```bash
# 如果应用暴露配置端点（可选）
curl -s http://localhost:8080/actuator/configprops | jq '.propertySources[]' | grep -E "aiExecutor|sentinel"

# 检查 env 变量是否覆盖
curl -s http://localhost:8080/actuator/env | jq '.propertySources[].source' | grep -E "SENTINEL|CORE_SIZE"
```

---

## 🔌 SSE 心跳验证

### 快速烟测（5s 内完成）

```bash
#!/bin/bash
# test-sse-quick.sh

USER_ID="test_user_$(date +%s%N | tail -c 5)"
SESSION_ID="sse_test_$(date +%s%N | tail -c 5)"

echo "测试 SSE 心跳..."
echo "User: $USER_ID, Session: $SESSION_ID"

timeout 5 curl -s -X POST http://localhost:8080/api/agent/stream \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "'$USER_ID'",
    "session_id": "'$SESSION_ID'",
    "message": "你好"
  }' | tee /tmp/sse_output.txt

echo ""
echo "事件统计:"
grep "^event:" /tmp/sse_output.txt | sort | uniq -c

# 期望输出:
#      1 event:queued
#      N event:heartbeat
#      1 event:message
#      1 event:end
```

运行：
```bash
bash /tmp/test-sse-quick.sh
```

---

## 💾 Redis 和缓存验证

```bash
# 1. Redis 连接检查
redis-cli ping
# 期望: PONG

# 2. 会话缓存键检查
redis-cli KEYS "agent:session:seen:*" | wc -l
# 期望: > 0 (表示有活跃会话)

# 3. 消息缓存键检查
redis-cli KEYS "agent:memory:recent:*" | wc -l

# 4. RAG 缓存命中率
redis-cli DBSIZE
# 期望: 显示数据库大小

# 5. 清空测试数据（小心使用）
redis-cli KEYS "agent:*test*" | xargs redis-cli DEL
redis-cli KEYS "test_session_*" | xargs redis-cli DEL
```

---

## 🗄️ 数据库连接池状态

```bash
# HikariCP 连接池指标
curl -s http://localhost:8080/actuator/metrics/hikaricp.connections \
  | jq '.measurements[] | {statistic, value}'

# 期望:
# {
#   "statistic": "VALUE",
#   "value": 12  (当前活跃连接)
# }

# 查看最大连接数配置
grep "maximum-pool-size" src/main/resources/application.yaml
# 期望: maximum-pool-size: 40
```

---

## 🚦 Sentinel 限流验证

```bash
# 1. 快速限流测试（发送 600 个请求，看有多少被限流）
bash << 'EOF'
SUCCESS=0
BLOCKED=0
for i in {1..600}; do
  RESPONSE=$(curl -s -X POST http://localhost:8080/api/agent/chat \
    -H "Content-Type: application/json" \
    -d '{
      "user_id": "user_'$((i % 10))'",
      "session_id": "session_'$i'",
      "message": "测试"
    }' -w "\n%{http_code}")
  
  HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
  if [ "$HTTP_CODE" = "200" ]; then
    SUCCESS=$((SUCCESS + 1))
  elif [ "$HTTP_CODE" = "429" ]; then
    BLOCKED=$((BLOCKED + 1))
  fi
  
  # 每 100 请求打印进度
  if [ $((i % 100)) -eq 0 ]; then
    echo "进度: $i, 成功: $SUCCESS, 限流: $BLOCKED"
  fi
done

echo "最终结果:"
echo "成功: $SUCCESS"
echo "限流: $BLOCKED"
EOF

# 期望: 在 500 QPS 限制下，大约 50% 会被限流（因为并发很高）
```

---

## 📈 吞吐量快速测试

```bash
# 使用 wrk 工具（需要预装）
# wrk -t4 -c50 -d10s -R500 http://localhost:8080/api/agent/health

# 如果没有 wrk，使用 Apache Bench
ab -n 1000 -c 50 http://localhost:8080/health

# 期望: 
# Requests per second: > 400 (至少接近之前的基线)
# Failed requests: 0
# Connection errors: 0
```

---

## 🔍 链路追踪验证

### Zipkin 手动验证

```bash
# 1. 检查 Zipkin 是否运行
curl -s http://localhost:9411/api/v2/services | jq '.'
# 期望: ["medicalassistant", ...] 列表

# 2. 查询最近的 trace
curl -s "http://localhost:9411/api/v2/traces?limit=1" | jq '.traces[0]'

# 3. 检查 span 数量和 tag
curl -s "http://localhost:9411/api/v2/traces?serviceName=medicalassistant&limit=5" \
  | jq '.traces[0].spans[] | {name, duration, tags}'

# 期望:
# {
#   "name": "http /api/agent/chat",
#   "duration": 250000,  # 微秒
#   "tags": {
#     "user_id": "...",
#     "session_id": "...",
#     "http.method": "POST",
#     "http.status_code": "200"
#   }
# }
```

### 通过 UI 验证

```bash
# 打开浏览器
open http://localhost:9411

# 操作步骤:
# 1. 左上角 Select a service → 选择 medicalassistant
# 2. 点击 Find Traces
# 3. 随机点击一个 trace 查看详情
# 4. 检查:
#    - Span 数量完整（chat → RAG → LLM）
#    - Timeline 显示各环节耗时
#    - Tags 包含业务信息（user_id, session_id, cache_hit）
```

---

## 🧪 小规模压力测试

### 场景 1: 50 并发，10s 持续

```bash
# 使用前面创建的测试脚本的简化版本
python3 << 'EOF'
import asyncio
import aiohttp
import time

async def test():
    async with aiohttp.ClientSession() as session:
        tasks = []
        for i in range(50):
            task = session.post(
                'http://localhost:8080/api/agent/chat',
                json={
                    'user_id': f'user_{i}',
                    'session_id': f'session_{i}_{int(time.time())}',
                    'message': f'测试消息 {i}'
                },
                timeout=aiohttp.ClientTimeout(total=30)
            )
            tasks.append(task)
        
        start = time.time()
        responses = await asyncio.gather(*tasks, return_exceptions=True)
        elapsed = time.time() - start
        
        success = sum(1 for r in responses if isinstance(r, int) or (hasattr(r, 'status') and r.status == 200))
        print(f"并发: 50, 耗时: {elapsed:.1f}s, 成功: {success}/50")

asyncio.run(test())
EOF
```

### 场景 2: 100 并发，30s 持续（验证 SSE 稳定性）

```bash
# 运行前面创建的 sse_longconn_test.py
# (修改参数: CONCURRENCY=100, 超时=30)

python3 test/perf/sse_longconn_test.py --concurrency 100 --wait-timeout 30
```

---

## 🛠️ 常见问题快速排查

### 问题 1: Health 检查返回 DOWN

```bash
# 检查日志
tail -100 logs/application.log | grep -E "ERROR|Exception"

# 常见原因:
# - MySQL 连接失败 → 检查 MYSQL_HOST/MYSQL_PORT
# - Redis 连接失败 → 检查 REDIS_HOST/REDIS_PORT
# - DashScope API key 缺失 → 检查 DASHSCOPE_API_KEY
```

### 问题 2: 心跳不规律或断连

```bash
# 检查 nginx 配置
grep -E "proxy_read_timeout|keepalive" nginx/nginx.microservices.conf

# 应该看到:
# proxy_read_timeout 300s;
# keepalive 64;

# 检查应用中的心跳配置
grep "15" src/main/java/com/whu/medicalbackend/agent/controller/AgentProxyController.java
# 应该有 15 秒的心跳间隔

# 检查日志中的心跳发送记录
grep "heartbeat" logs/application.log | tail -20
```

### 问题 3: 限流过于严格（429 太多）

```bash
# 检查当前 Sentinel 限流值
grep "chat-qps" src/main/resources/application.yaml

# 临时增加限流（通过环境变量）
export SENTINEL_CHAT_QPS=2000
# 重启应用

# 或在 docker-compose 中修改:
# environment:
#   SENTINEL_CHAT_QPS: 2000
```

### 问题 4: 缓存未命中（总是慢请求）

```bash
# 检查 Redis 中是否有缓存
redis-cli KEYS "agent:memory:*" | head -5

# 检查 RAG 缓存是否启用
grep "rag:" src/main/resources/application.yaml -A 15 | grep "enabled"
# 应该是 enabled: true

# 强制清空缓存并重新测试
redis-cli FLUSHDB
# 再执行一次相同问题，应该会被缓存
```

---

## 📋 日常部署前检查清单

```
□ 配置文件验证
  □ application.yaml 所有占位符已填充
  □ 没有 TODO 注释
  □ 敏感信息（密码）通过环境变量注入

□ 依赖检查
  □ Maven 构建成功 (mvn clean install)
  □ 无编译错误和 warning
  □ 单元测试通过

□ 功能验证
  □ Health 检查通过
  □ 核心业务流程可用
  □ SSE 心跳正常

□ 性能基线
  □ 100 并发 Agent Chat p50 < 500ms
  □ 50 并发 SSE max 等待 < 60s
  □ 无 502 / 504 错误

□ 可观测性
  □ Zipkin 正常接收 span
  □ 日志输出格式正确（含 traceId）
  □ Metrics 端点可访问

□ 安全性
  □ 无 SQL 注入风险（parameterized queries）
  □ API 鉴权开启
  □ 敏感信息不输出日志
```

---

## 🔗 相关文件快速导航

| 用途 | 文件 | 关键行 |
|------|------|--------|
| 应用配置 | `src/main/resources/application.yaml` | 123-140 (ai executor) |
| Sentinel 限流 | `src/main/java/.../SentinelConfig.java` | L20-40 |
| SSE 心跳 | `src/main/java/.../AgentProxyController.java` | L100-150 |
| nginx 配置 | `nginx/nginx.microservices.conf` | L30-60 (upstream) |
| 链路追踪 | `src/main/java/.../TracingConfig.java` | 全文 |

---

## 📞 快速求助

**问题**：系统无法启动  
**解决**：
```bash
# 1. 清理编译缓存
mvn clean

# 2. 查看错误日志
mvn spring-boot:run | grep -E "ERROR|Exception"

# 3. 检查端口占用
lsof -i :8080

# 4. 重新构建
mvn install -DskipTests
```

**问题**：SSE 连接频繁断开  
**解决**：
```bash
# 1. 查看 nginx 日志
tail -50 /var/log/nginx/error.log | grep timeout

# 2. 增加 proxy_read_timeout
# 编辑 nginx/nginx.microservices.conf，改为 300s

# 3. 检查应用心跳是否发送
grep "heartbeat" application.log | tail -5
```

**问题**：大量 429 限流错误  
**解决**：
```bash
# 1. 检查当前限流值
echo "SENTINEL_CHAT_QPS=${SENTINEL_CHAT_QPS:-500}"

# 2. 临时增加限流
export SENTINEL_CHAT_QPS=2000

# 3. 观察是否仍有 429
curl -X POST http://localhost:8080/api/agent/chat ... | jq '.status'
```

---

**最后更新**: 2026-05-21  
**使用场景**: 日常开发、测试、上线前检查  
**相关文档**: [improvements-summary-2026-05.md](improvements-summary-2026-05.md), [optimization-plan-phase2.md](optimization-plan-phase2.md)
