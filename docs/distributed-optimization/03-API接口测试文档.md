# API 接口测试文档

> 基于 14 个 Controller、39 个接口的实际代码分析
> 测试环境：docker-compose.microservices.yml（端口 8080 via Nginx）

---

## 测试前置条件

1. Docker 启动：`docker-compose -f docker-compose.microservices.yml up -d`
2. 等待所有服务健康（约 30-60 秒）
3. 基础 URL：`http://localhost:80`（Nginx 代理）

---

## 一、用户模块 (UserController)

### T1.1 用户注册 [无需认证]

```bash
curl -X POST http://localhost:80/api/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser01",
    "password": "Test123456",
    "nickname": "测试用户",
    "phoneNumber": "13800138001"
  }'
```

**预期**：`{"code":200, "data":{userId, username, nickname, token, refreshToken}, "message":"success"}`

---

### T1.2 用户登录 [无需认证]

```bash
curl -X POST http://localhost:80/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser01",
    "password": "Test123456"
  }'
```

**预期**：返回 UserVO 含 `token` 和 `refreshToken`，后续接口使用 `Authorization: Bearer {token}`

---

### T1.3 修改用户信息 [需要认证]

```bash
curl -X PUT http://localhost:80/api/user/modify \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "id": {USER_ID},
    "username": "testuser01",
    "newNickname": "新昵称",
    "newPassword": "",
    "newPhoneNumber": "13800138001"
  }'
```

---

### T1.4 刷新 Token [无需认证]

```bash
curl -X POST http://localhost:80/api/user/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "{USER_ID}",
    "refreshToken": "{REFRESH_TOKEN}"
  }'
```

---

### T1.5 随机头像 [无需认证]

```bash
curl -I http://localhost:80/api/user/avatar/random
```

**预期**：302 重定向到 `/avatar/file/avatar_N.svg`

---

## 二、家庭组模块 (FamilyGroupController) [全部需认证]

### T2.1 创建家庭组

```bash
curl -X POST http://localhost:80/api/family/group \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "groupName": "我的家庭",
    "description": "家庭健康管理"
  }'
```

---

### T2.2 查询我的家庭

```bash
curl http://localhost:80/api/family/group/me \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T2.3 邀请家庭成员

```bash
curl -X POST "http://localhost:80/api/family/group/{GROUP_ID}/invite?phoneNumber=13900139001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{"remark": "加入我的家庭"}'
```

---

### T2.4 审批邀请

```bash
curl -X POST http://localhost:80/api/family/group/{GROUP_ID}/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "applyId": {APPLY_ID},
    "opType": "approve",
    "remark": "同意加入"
  }'
```

---

### T2.5 退出家庭

```bash
curl -X POST http://localhost:80/api/family/group/{GROUP_ID}/quit \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T2.6 查询申请记录

```bash
curl http://localhost:80/api/family/group/my/apply-records \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T2.7 查询待审批列表

```bash
curl http://localhost:80/api/family/group/{GROUP_ID}/pending-applies \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T2.8 家庭健康快照

```bash
curl http://localhost:80/api/family/group/{GROUP_ID}/health \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T2.9 家庭告警列表

```bash
curl http://localhost:80/api/family/group/{GROUP_ID}/alarms \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 三、医疗模块 (MedicineController) [全部需认证]

### T3.1 查询药品列表

```bash
curl http://localhost:80/api/medicine \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T3.2 添加药品

```bash
curl -X POST http://localhost:80/api/medicine \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "name": "阿莫西林",
    "defaultDosage": "500mg",
    "remark": "饭后服用"
  }'
```

---

### T3.3 修改药品

```bash
curl -X PUT http://localhost:80/api/medicine/{MEDICINE_ID} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "name": "阿莫西林胶囊",
    "defaultDosage": "250mg",
    "remark": "饭后服用，一日三次"
  }'
```

---

### T3.4 删除药品

```bash
curl -X DELETE http://localhost:80/api/medicine/{MEDICINE_ID} \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T3.5 从药品创建用药计划

```bash
curl -X POST http://localhost:80/api/medicine/{MEDICINE_ID}/plan \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "dosage": "500mg",
    "startDate": "2026-05-10",
    "endDate": "2026-06-10",
    "timePoints": ["08:00", "12:00", "20:00"],
    "remark": "连续服用30天"
  }'
```

---

## 四、计划模块 (PlanController) [全部需认证]

### T4.1 查询计划列表

```bash
curl http://localhost:80/api/plan \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T4.2 创建用药计划

```bash
curl -X POST http://localhost:80/api/plan \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "medicineName": "布洛芬",
    "dosage": "400mg",
    "startDate": "2026-05-10",
    "endDate": "2026-05-20",
    "timePoints": ["08:00", "20:00"],
    "remark": "退烧用"
  }'
```

---

### T4.3 修改计划

```bash
curl -X PUT http://localhost:80/api/plan/{PLAN_ID} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "medicineName": "布洛芬",
    "dosage": "200mg",
    "startDate": "2026-05-10",
    "endDate": "2026-05-25",
    "timePoints": ["08:00", "14:00", "20:00"],
    "remark": "调整剂量"
  }'
```

---

### T4.4 删除计划

```bash
curl -X DELETE http://localhost:80/api/plan/{PLAN_ID} \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 五、任务模块 (TaskController) [全部需认证]

### T5.1 查询今日任务

```bash
curl http://localhost:80/api/task/today \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T5.2 更新任务状态

```bash
curl -X PUT http://localhost:80/api/task/{TASK_ID}/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{"status": 1}'
```

**status**: 0=未服药, 1=已服药, 2=漏服

---

### T5.3 查询历史任务

```bash
curl "http://localhost:80/api/task/history?start=2026-05-01&end=2026-05-31&status=1" \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 六、健康数据模块 (HealthController) [需认证]

### T6.1 同步健康数据

```bash
curl -X POST http://localhost:80/api/health/sync \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "userId": {USER_ID},
    "heartRate": 72.5,
    "stepCount": 8500,
    "sleepDuration": 7.5,
    "sleepScope": 2,
    "bloodOxygen": 98.0,
    "measureTime": "2026-05-09T10:00:00"
  }'
```

---

### T6.2 查询最新健康数据

```bash
curl http://localhost:80/api/health/latest \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 七、AI Agent 模块 [大部分无需认证]

### T7.1 Agent 健康检查 [无需认证]

```bash
curl http://localhost:80/api/agent/health
```

---

### T7.2 普通对话 [无需认证]

```bash
curl -X POST http://localhost:80/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "1",
    "session_id": "test-session-001",
    "message": "你好，我最近总忘记吃药，有什么建议吗？"
  }'
```

---

### T7.3 流式对话(SSE) [无需认证]

```bash
curl -N "http://localhost:80/api/agent/chat/stream?user_id=1&session_id=test-session-002&message=帮我查一下今天的用药任务"
```

---

### T7.4 会话列表 [无需认证]

```bash
curl "http://localhost:80/api/agent/sessions?userId=1"
```

---

### T7.5 创建会话 [无需认证]

```bash
curl -X POST http://localhost:80/api/agent/sessions \
  -H "Content-Type: application/json" \
  -d '{"userId": "1"}'
```

---

### T7.6 会话历史消息 [无需认证]

```bash
curl "http://localhost:80/api/agent/sessions/{SESSION_ID}/messages?limit=50"
```

---

### T7.7 更新会话摘要 [无需认证]

```bash
curl -X PUT http://localhost:80/api/agent/sessions/{SESSION_ID} \
  -H "Content-Type: application/json" \
  -d '{"summary": "讨论了用药计划"}'
```

---

### T7.8 删除会话 [无需认证]

```bash
curl -X DELETE http://localhost:80/api/agent/sessions/{SESSION_ID}
```

---

### T7.9 删除会话所有消息 [无需认证]

```bash
curl -X DELETE http://localhost:80/api/agent/sessions/{SESSION_ID}/messages
```

---

### T7.10 Tool 执行审批 - 查询待审批 [需认证]

```bash
curl "http://localhost:80/api/agent/tool-execution/pending?userId={USER_ID}" \
  -H "Authorization: Bearer {TOKEN}"
```

---

### T7.11 Tool 执行审批 - 批准 [需认证]

```bash
curl -X POST "http://localhost:80/api/agent/tool-execution/approve?userId={USER_ID}&requestId={REQUEST_ID}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{}'
```

---

### T7.12 Tool 执行审批 - 拒绝 [需认证]

```bash
curl -X POST "http://localhost:80/api/agent/tool-execution/reject?userId={USER_ID}&requestId={REQUEST_ID}" \
  -H "Authorization: Bearer {TOKEN}"
```

---

## 八、OCR 模块 [无需认证]

### T8.1 OCR 健康检查

```bash
curl http://localhost:80/api/ocr/health
```

---

### T8.2 OCR 药品识别

```bash
curl -X POST http://localhost:80/api/ocr/test \
  -F "file=@/path/to/drug_image.jpg"
```

---

## 九、RAG 模块 [无需认证]

### T9.1 RAG 查询

```bash
curl -X POST http://localhost:80/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "高血压患者应该注意什么？",
    "with_trace": false,
    "with_timing": false
  }'
```

---

## 十、预测模块 [无需认证]

### T10.1 药物不良反应预测

```bash
curl -X POST http://localhost:80/api/predict/analyze \
  -H "Content-Type: application/json" \
  -d '{"text": "患者服用阿莫西林后出现皮疹和瘙痒"}'
```

---

## 十一、PDF 模块

### T11.1 生成就医准备单 [需认证]

```bash
curl -X POST http://localhost:80/api/medical/prepare/pdf \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {TOKEN}" \
  -d '{
    "generatedTime": "2026-05-09 10:00",
    "department": "心内科",
    "patient": "张三",
    "visitDate": "2026-05-15",
    "medications": [
      {"id": 1, "name": "阿莫西林", "schedule": "每日三次", "takenDays": 10, "missedCount": 1, "status": "服药中"}
    ],
    "healthData": [
      {"id": 1, "date": "2026-05-08", "indicator": "心率", "value": "72", "unit": "bpm", "status": "正常", "isAbnormal": false}
    ],
    "questions": ["最近血压偏高是否需要调整用药？"],
    "otherInfo": "无"
  }'
```

---

## 测试执行顺序

建议按以下顺序测试（有依赖关系）：

```
1. T1.1 注册 → T1.2 登录（获取 TOKEN）
2. T2.1 创建家庭 → T2.2 查询 → T2.3 邀请
3. T3.2 添加药品 → T3.1 查询列表
4. T4.2 创建计划 → T4.1 查询列表 → T5.1 查询今日任务
5. T5.2 更新任务状态 → T5.3 查询历史
6. T6.1 同步健康数据 → T6.2 查询最新
7. T7.1~T7.9 Agent 相关（独立测试）
8. T8~T10 OCR/RAG/Predict（独立测试）
9. T11.1 PDF 生成
```

---

## 注意事项

- `{TOKEN}`：登录后从返回的 `token` 字段获取
- `{USER_ID}`：注册/登录后从返回的 `userId` 字段获取
- `{GROUP_ID}`：创建家庭组后从返回的 `groupId` 字段获取
- `{MEDICINE_ID}`：添加药品后从返回的 `medicineId` 字段获取
- `{PLAN_ID}`：创建计划后从返回的 `planId` 字段获取
- `{TASK_ID}`：查询今日任务后从返回的 `taskId` 字段获取
- `{SESSION_ID}`：创建会话后从返回的 `sessionId` 字段获取
- 所有需要认证的接口，Header 格式为 `Authorization: Bearer {TOKEN}`

---

## 十二、U6 延迟任务可靠性 — 专项验证

> 验证改进点：U6.1 持久化、U6.2 重试+DLQ、U6.3 监控

### 前置：执行基础流程

先完成基础数据准备（按顺序执行）：

```bash
# 1. 注册 + 登录
curl -X POST http://localhost:80/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"u6tester","password":"Test123456","nickname":"U6测试","phoneNumber":"13800138901"}'

# 保存 TOKEN 和 USER_ID
TOKEN="<从返回中取 token>"
USER_ID="<从返回中取 userId>"

# 2. 添加药品
curl -X POST http://localhost:80/api/medicine \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"维生素C","defaultDosage":"1片","remark":"测试药品"}'
MEDICINE_ID="<从返回中取 medicineId>"

# 3. 获取当前时间+5分钟的时间点（让延迟任务在近期触发）
# 例如现在是 10:03，则用 "10:08" 作为服药时间点
```

---

### T12.1 — U6.1 延迟任务持久化：创建计划后任务入 Redis ZSET

```bash
# 创建计划（用即将到来的时间点做 timePoints）
curl -X POST http://localhost:80/api/plan \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"medicineName\": \"维生素C\",
    \"dosage\": \"1片\",
    \"startDate\": \"$(date +%Y-%m-%d)\",
    \"endDate\": \"$(date -d '+7 days' +%Y-%m-%d)\",
    \"timePoints\": [\"$(date -d '+5 minutes' +%H:%M)\", \"$(date -d '+10 minutes' +%H:%M)\"],
    \"remark\": \"U6延迟任务测试\"
  }"
PLAN_ID="<从返回中取 planId>"
```

**验证点 A — Redis ZSET 中有任务**:

```bash
docker exec redis redis-cli ZRANGE infra:delay:tasks 0 -1 WITHSCORES | head -20
```

预期：至少 4 条 JSON（2个时间点 × 2种类型 remind+missed）：
- `medication.remind` × 2 — 提醒（服药前5分钟）
- `medication.missed` × 2 — 漏服标记（服药后2分钟）

**验证点 B — 监控接口能看到**:

```bash
curl -s http://localhost:80/api/admin/delay-tasks/status | python3 -m json.tool
```

预期 `pendingTasks > 0`。

**验证点 C — 重启后任务仍在**:

```bash
# 重启应用（Ctrl+C 后重新 mvn spring-boot:run）
# 然后立即查询 Redis（重启过程中到期的任务会被消费，但未来的还在）
docker exec redis redis-cli ZRANGE infra:delay:tasks 0 -1 WITHSCORES
```

预期：未来时间点的延迟任务仍在 Redis 中，不会因重启丢失。

---

### T12.2 — U6.1 任务取消：标记完成后取消延迟任务

```bash
# 查看今日任务
curl -s http://localhost:80/api/task/today \
  -H "Authorization: Bearer $TOKEN"
TASK_ID="<取返回中的 taskId>"

# 标记为已服用（触发 cancelTaskSchedule）
curl -X PUT http://localhost:80/api/task/$TASK_ID/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"status": 1}'
```

**验证点 — 取消标记写入 Redis**:

```bash
docker exec redis redis-cli SMEMBERS infra:delay:canceled
```

预期：包含 `medication.missed:{TASK_ID}` 和 `medication.remind:{TASK_ID}`。

---

### T12.3 — U6.1 邀请过期延迟任务

```bash
# 创建家庭组
curl -X POST http://localhost:80/api/family/group \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"groupName":"U6测试家庭","description":"延迟任务测试"}'
GROUP_ID="<从返回中取 groupId>"

# 第二个用户注册
curl -X POST http://localhost:80/api/user/register \
  -H "Content-Type: application/json" \
  -d '{"username":"u6tester2","password":"Test123456","nickname":"U6测试2","phoneNumber":"13900139901"}'
TOKEN2="<从返回中取 token>"

# 向第二个用户发起邀请（创建 48 小时过期延迟任务）
curl -X POST "http://localhost:80/api/family/group/$GROUP_ID/invite?phoneNumber=13900139901" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"remark":"测试邀请过期"}'
```

**验证点 — Redis ZSET 中有邀请过期任务**:

```bash
docker exec redis redis-cli ZRANGE infra:delay:tasks 0 -1 WITHSCORES | grep "family.invite.expire"
```

预期：1 条 `"family.invite.expire"` 任务，executeAt = 当前时间 + 48小时。

---

### T12.4 — U6.2 失败重试与死信队列

重试需要 Handler 抛出异常。可以通过临时手段验证 DLQ 通路：

**验证点 A — RabbitMQ DLX Exchange 存在**:

浏览器打开 `http://localhost:15672`（guest/guest）→ Exchanges → 确认 `medical.dlx.topic` 存在。

**验证点 B — RabbitMQ 死信队列存在**:

Queues 页面 → 确认 `queue.dead.letter` 存在且绑定到 `medical.dlx.topic`。

**验证点 C — 重试逻辑代码审查确认**（此场景难以通过 API 直接触发，建议代码审查验证）:

在 [RedisDelayTaskConsumer.java](src/main/java/com/whu/medicalbackend/common/infra/delay/RedisDelayTaskConsumer.java) 中：
- 第 112-123 行 `retryOrDiscard()`：重试次数未超 `maxRetry(3)` 时，指数退避后重新加入 ZSET
- 第 115 行：重试耗尽调用 `publishToDeadLetter()` 投递到 `medical.dlx.topic`
- 第 152-153 行 `backoff()`：`2^retryCount` 秒，上限 60 秒

---

### T12.5 — U6.3 监控接口完整验证

```bash
# 完整状态查询
curl -s http://localhost:80/api/admin/delay-tasks/status | python3 -m json.tool
```

返回字段含义：

| 字段 | 含义 | 正常预期 |
|------|------|---------|
| `pendingTasks` | Redis ZSET 中等待执行的任务数 | > 0（有未来任务时）|
| `overdueTasks` | 已到期但尚未消费的任务数 | 0（正常情况消费及时）|
| `canceledCount` | 已被取消的任务标记数 | ≥ 0 |

正常情况 `overdueTasks` 应为 0，如果持续 > 0 说明消费者处理速度跟不上或消费者未启动。

---

### T12.6 — 回归验证：业务接口行为不变

改进后，以下业务流程仍正常（执行顺序测试）：

```bash
# 1. 创建计划 → 生成任务 → 延迟任务自动发布
T4.2 创建计划 → T5.1 查询今日任务（任务应有 status=0）

# 2. 标记已服用 → 取消延迟任务
T5.2 更新任务状态 status=1

# 3. 标记漏服 → 取消延迟任务（用户手动标记，不等到期）
T5.2 更新任务状态 status=2

# 4. 删除计划 → 取消今日任务的延迟任务
T4.4 删除计划

# 5. 编辑计划 → 取消旧延迟任务
T4.3 修改计划
```

每个步骤后检查：
```bash
curl -s http://localhost:80/api/admin/delay-tasks/status
```

预期：任务数随操作正常增减，不出现异常堆积。

---

## U6 验收清单

| # | 验证项 | 测试场景 | 通过标准 |
|---|--------|---------|---------|
| 1 | 延迟任务持久化 | T12.1 | 创建计划后 Redis ZSET 有对应 JSON 记录 |
| 2 | 重启恢复 | T12.1-C | 重启应用后 ZSET 中未到期任务仍存在 |
| 3 | 任务取消 | T12.2 | 标记完成后 canceled set 有对应标记 |
| 4 | 邀请过期 | T12.3 | 发起邀请后 ZSET 有 family.invite.expire |
| 5 | 死信队列 | T12.4 | RabbitMQ `queue.dead.letter` 绑定了 `medical.dlx.topic` |
| 6 | 监控接口 | T12.5 | `/api/admin/delay-tasks/status` 返回正确统计 |
| 7 | 业务兼容 | T12.6 | 创建计划/标记任务/删除计划流程正常 |
| 8 | Lua 原子性 | 代码审查 | `POP_READY_TASKS_SCRIPT` 原子执行 ZRANGEBYSCORE + ZREM |
| 9 | 指数退避 | 代码审查 | `backoff(n) = min(60, 2^n)` 秒，最多 3 次重试 |
| 10 | 无内存池残留 | 代码审查 | `DynamicTaskScheduler` 不再包含 ConcurrentHashMap |
