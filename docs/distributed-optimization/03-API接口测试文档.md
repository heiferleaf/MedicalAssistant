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
