# 模块五 Apifox 压力测试指南

## 一、测试环境

```
服务器:   http://localhost:80 （Nginx 反向代理）
鉴权:     全部 Agent 接口均免 Token
部署:     docker compose -f docker-compose.microservices.yml up -d
环境变量: 在项目根目录创建 .env 文件，写入 DASHSCOPE_API_KEY=你的key
```

---

## 二、Apifox 自动化测试配置

### 2.1 创建环境变量

打开 Apifox → **环境管理** → 新建环境 `Module5-Test`，添加以下变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `base_url` | `http://localhost:80` | 服务器地址（Nginx 入口） |
| `user_id` | `1` | 测试用户 ID |
| `session_id` | 空 | 由创建会话接口自动填充 |
| `session_id_2` | 空 | 备用会话 ID |
| `task_id` | 空 | 由异步任务接口自动填充 |
| `message` | `我今天头痛，有什么建议吗？` | 测试消息 |

### 2.2 配置后置操作（自动提取变量）

每个接口在 Apifox 中需要配置"后置操作"来自动提取返回值到环境变量。以下按接口逐一说明。

---

## 三、接口清单（含后置操作配置）

### 接口 1：探活 Health

```
GET {{base_url}}/api/agent/health
```

- **请求方式**: GET
- **请求参数**: 无
- **后置操作**: 不需要
- **预期结果**:
  ```json
  {"code":200,"data":{"status":"ok","module":"agent"}}
  ```

---

### 接口 2：创建会话 ← 从这里提取 session_id

```
POST {{base_url}}/api/agent/sessions
Content-Type: application/json

{"userId":"{{user_id}}"}
```

- **请求方式**: POST
- **请求参数（Body）**:
  - `userId`（String，必填）：用户 ID
- **后置操作**（关键！自动提取 session_id）：

```javascript
// 提取 sessionId 到环境变量
var json = pm.response.json();
if (json.code === 200 && json.data && json.data.sessionId) {
    pm.environment.set("session_id", json.data.sessionId);
    console.log("已自动设置 session_id = " + json.data.sessionId);
}
```

- **在 Apifox 中的配置位置**：接口编辑 → 后置操作 → 添加自定义脚本 → 粘贴上述代码
- **预期结果**:
  ```json
  {"code":200,"data":{"sessionId":"xxx","userId":"1"}}
  ```

---

### 接口 3：获取会话列表（Query 传参）

```
GET {{base_url}}/api/agent/sessions?userId={{user_id}}
```

- **请求方式**: GET
- **请求参数（Query）**:
  - `userId`（String，必填）：用户 ID
- **注意**: 参数是 Query 参数，**不要**放到请求 Body 里
- **后置操作**: 不需要
- **预期结果**:
  ```json
  {"code":200,"data":[{"sessionId":"xxx","userId":"1"}]}
  ```

---

### 接口 4：更新会话（Path + Body）

```
PUT {{base_url}}/api/agent/sessions/{{session_id}}
Content-Type: application/json

{"summary":"患者高血压随访记录"}
```

- **请求方式**: PUT
- **请求参数**:
  - Path: `session_id` 使用 `{{session_id}}` 变量（由接口 2 自动填充）
  - Body: `{"summary":"任意字符串"}`
- **后置操作**: 不需要
- **预期结果**:
  ```json
  {"code":200,"data":{"summary":"xxx","sessionId":"xxx"}}
  ```

---

### 接口 5：删除会话

```
DELETE {{base_url}}/api/agent/sessions/{{session_id}}
```

- **请求方式**: DELETE
- **请求参数**: Path 中使用 `{{session_id}}`
- **后置操作**: 不需要
- **预期结果**: `{"code":200,"data":null}`

---

### 接口 6：获取会话消息

```
GET {{base_url}}/api/agent/sessions/{{session_id}}/messages?limit=50
```

- **请求方式**: GET
- **请求参数**:
  - Path: `session_id` 使用 `{{session_id}}`
  - Query: `limit=50`
- **后置操作**: 不需要
- **预期结果**:
  ```json
  {"code":200,"data":[{"role":"user","content":"..."}]}
  ```

---

### 接口 7：清空会话消息

```
DELETE {{base_url}}/api/agent/sessions/{{session_id}}/messages
```

- **请求方式**: DELETE
- **请求参数**: Path 中使用 `{{session_id}}`
- **预期结果**: `{"code":200,"data":null}`

---

### 接口 8：同步 Chat（核心接口）

```
POST {{base_url}}/api/agent/chat
Content-Type: application/json

{
  "user_id": "{{user_id}}",
  "session_id": "{{session_id}}",
  "message": "{{message}}",
  "with_trace": true
}
```

- **请求方式**: POST
- **请求参数（Body）**:
  - `user_id`（String，必填）
  - `session_id`（String，必填）
  - `message`（String，必填）
  - `with_trace`（Boolean，可选）
- **后置操作**: 不需要
- **预期结果**（LLM 未配置时的降级响应）:
  ```json
  {"code":200,"data":{"success":false,"message":"LLM 未配置，请配置 DASHSCOPE_API_KEY 后重试"}}
  ```

---

### 接口 9：异步 Chat ← 从这里提取 task_id

#### 9.1 提交异步任务

```
POST {{base_url}}/api/agent/chat/async
Content-Type: application/json

{
  "user_id": "{{user_id}}",
  "session_id": "{{session_id}}",
  "message": "{{message}}"
}
```

- **请求方式**: POST
- **后置操作**（提取 task_id）：

```javascript
var json = pm.response.json();
if (json.code === 200 && json.data && json.data.taskId) {
    pm.environment.set("task_id", json.data.taskId);
    console.log("已自动设置 task_id = " + json.data.taskId);
    console.log("任务等级 = " + json.data.grade);
    console.log("超时时间 = " + json.data.timeoutMs + "ms");
}
```

- **预期结果**:
  ```json
  {"code":200,"data":{"taskId":"xxx","grade":"L2_SIMPLE_LLM","status":"QUEUED","timeoutMs":10000}}
  ```
- **不同消息长度触发的 grade**:
  - 短消息（<500 字符）→ `L2_SIMPLE_LLM`，超时 10s
  - 中等长度（500-2000 字符）→ `L3_AGENT_MULTI`，超时 30s
  - 长消息（>2000 字符）或含图片 → `L4_LONG_RUNNING`，超时 120s

#### 9.2 轮询任务结果

```
GET {{base_url}}/api/agent/task/{{task_id}}
```

- **请求方式**: GET
- **请求参数**: Path 中使用 `{{task_id}}`（由 9.1 自动填充）
- **后置操作**: 不需要
- **预期结果**:
  ```json
  {"code":200,"data":{"taskId":"xxx","status":"SUCCESS","grade":"L2_SIMPLE_LLM"}}
  ```
- **status 含义**:
  - `QUEUED` → 排队中，继续轮询
  - `RUNNING` → 执行中，继续轮询
  - `SUCCESS` → 已完成
  - `FAILED` → 执行失败
  - `TIMEOUT` → 超时取消

#### 9.3 查询不存在的任务（边界测试）

```
GET {{base_url}}/api/agent/task/nonexistent-task-id
```

- **预期结果**: `{"code":400,"message":"任务不存在或已过期","data":null}`

---

### 接口 10：SSE 流式 Chat

```
GET {{base_url}}/api/agent/chat/stream?user_id={{user_id}}&session_id={{session_id}}&message={{message}}
```

- **请求方式**: GET
- **请求参数（全部 Query）**:
  - `user_id`（必填）
  - `session_id`（必填）
  - `message`（必填，Apifox 会自动 URL 编码）
- **后置操作**: 不需要
- **预期结果**: 流式返回 SSE 事件
  ```
  event:message
  data:LLM 未配置，当前无法进行智能问答。请配置 DASHSCOPE_API_KEY 后重试。
  
  event:end
  data:
  ```

---

### 接口 11：Tool 执行待确认

#### 11.1 获取待确认列表

```
GET {{base_url}}/api/agent/tool-execution/pending?userId={{user_id}}
```

- **请求方式**: GET
- **请求参数（Query）**: `userId={{user_id}}`
- **预期结果**: `{"success":true,"data":[],"count":0}`

#### 11.2 批准待确认请求

```
POST {{base_url}}/api/agent/tool-execution/approve?userId={{user_id}}&requestId={requestId}
Content-Type: application/json

{}
```

#### 11.3 拒绝待确认请求

```
POST {{base_url}}/api/agent/tool-execution/reject?userId={{user_id}}&requestId={requestId}
```

#### 11.4 清理过期待确认

```
POST {{base_url}}/api/agent/tool-execution/cleanup
```

#### 11.5 删除用户所有待确认

```
POST {{base_url}}/api/agent/tool-execution/delete-all?userId={{user_id}}
```

---

## 四、自动化测试流程（Apifox 场景测试）

### 4.1 配置步骤（在 Apifox 中创建一个"场景"）

1. 打开 Apifox → **自动化测试** → **新建场景**
2. 选择环境 `Module5-Test`
3. 按以下顺序添加接口，Apifox 会自动按顺序执行，变量会从上一步传递到下一步：

```
步骤 1: Health（探活）
步骤 2: 创建会话（后置操作自动设置 session_id）
步骤 3: 获取会话列表
步骤 4: 更新会话（自动使用步骤 2 的 session_id）
步骤 5: 同步 Chat
步骤 6: 异步 Chat（后置操作自动设置 task_id）
步骤 7: 轮询任务结果（自动使用步骤 6 的 task_id）
步骤 8: 获取会话消息
步骤 9: 清理待确认
```

### 4.2 场景变量传递原理

```
步骤2 (POST /sessions) → 后置脚本提取 session_id → 环境变量 {{session_id}}
步骤4 (PUT /sessions/{{session_id}})  ← 自动读取环境变量
步骤5 (POST /chat)  ← 自动读取 {{session_id}}
步骤6 (POST /chat/async) → 后置脚本提取 task_id → 环境变量 {{task_id}}
步骤7 (GET /task/{{task_id}})  ← 自动读取环境变量
```

整个过程无需手动复制任何 ID，全自动串联。

---

## 五、随机消息配置（避免缓存命中干扰）

压力测试时所有 Chat 请求使用相同 `{{message}}` 会导致：
- 如果有缓存，后续请求全命中缓存，测不出 LLM 真实耗时
- 不符合真实场景（用户不会问一模一样的问题）

需要在同步 Chat 和 SSE 接口的**前置操作**中添加随机消息脚本。以下提供三个版本，根据测试阶段选择。

### 版本 A：短消息（压同步 Chat 用）

每条消息 10-30 字，模拟快速问答场景，所有请求等级为 `L2_SIMPLE_LLM`。

```javascript
// 前置操作 - 版本 A：短消息（同步 Chat 压测用）
var messages = [
    "我今天头痛",
    "最近总是失眠",
    "肚子疼怎么办",
    "咳嗽好几天了",
    "发烧38度",
    "经常头晕",
    "胃酸过多",
    "血压有点高",
    "嗓子疼怎么办",
    "皮肤过敏起红疹",
    "眼睛干涩怎么办",
    "腰酸背痛",
    "腿抽筋是什么原因",
    "口腔溃疡",
    "便秘怎么办",
    "拉肚子好几天",
    "心跳有点快",
    "记忆力下降",
    "耳鸣怎么办",
    "关节痛"
];
var randomMsg = messages[Math.floor(Math.random() * messages.length)];
pm.environment.set("message", randomMsg);
console.log("本次随机短消息: " + randomMsg);
```

### 版本 B：长消息（压异步 Chat / SSE 用）

每条消息 200-500 字，包含详细病情描述，触发 `L3_AGENT_MULTI` 或 `L4_LONG_RUNNING` 等级。

```javascript
// 前置操作 - 版本 B：长消息（异步/SSE 压测用）
var messages = [
    "最近一周我经常头痛，位置在太阳穴附近，感觉像血管在跳动。通常在下午加重，有时候还会恶心想吐。平时工作压力大，经常熬夜到凌晨一两点。请问这是什么问题？需要去医院做检查吗？",
    "我失眠已经持续两周了，每天晚上躺在床上翻来覆去至少一个小时才能睡着，而且半夜总是醒两三次。白天精神很差，头晕脑胀的，工作效率明显下降。之前试过喝牛奶、泡脚都没什么效果，请问有没有什么好的建议？",
    "最近三天肚子一直不舒服，隐隐作痛，位置在肚脐周围。伴有腹泻，每天三四次，大便稀水样。没有发烧，但感觉浑身没力气。上周五在外面吃了顿麻辣火锅，不知道是不是这个原因。需要吃点什么药？",
    "咳嗽已经持续一周了，干咳为主，偶尔有少量白痰。晚上躺下的时候咳得更厉害，影响睡眠。没有发烧，但是感觉喉咙痒痒的，总是想清嗓子。之前吃过枇杷膏和止咳糖浆，效果不太明显。请问还需要继续吃药吗？",
    "我父亲今年65岁，有高血压病史，最近量血压一直在150/95左右。他平时吃的降压药是硝苯地平，每天早上吃一片。最近天气转凉，血压波动比较大，请问是否需要调整用药？另外日常生活中有哪些需要注意的地方？",
    "最近经常头晕，特别是早上起床和突然站起来的时候，眼前会发黑，持续几秒钟才能恢复。测量血压偏低，有时候只有90/60。平时饮食比较清淡，不吸烟不喝酒。请问这种情况需要治疗吗？还是通过饮食调理就可以了？",
    "我的胃反酸问题已经持续好几个月了，饭后尤其严重，有时候晚上躺下会感觉胃酸涌到喉咙，火辣辣的。之前做过胃镜，诊断是反流性食管炎，吃过奥美拉唑，停药后又复发了。长期吃这个药安全吗？有没有什么根治的方法？",
    "最近体检发现血脂偏高，总胆固醇6.8，甘油三酯3.2。我平时饮食偏油腻，爱吃肉，运动也比较少。医生建议先调整生活方式再复查。请问饮食上具体应该怎么调整？有哪些食物可以帮助降低血脂？需要吃药吗？",
    "我母亲最近检查出甲状腺结节，B超显示大小约1.5厘米，边界清晰，TI-RADS分级3类。她今年55岁，已经绝经了。平时没什么不舒服的感觉，就是在体检时偶然发现的。医生说定期复查就行，但我们还是有点担心。请问这个风险大吗？需要注意什么？",
    "最近一个多月来，我的右膝盖在上下楼梯时会疼痛，走平路还好。没有受过外伤，就是每天走路比较多，大概一万步左右。按压膝盖内侧会有痛感，有时候膝盖会发出咔咔的响声。请问这是关节炎吗？需要做什么检查？"
];
var randomMsg = messages[Math.floor(Math.random() * messages.length)];
pm.environment.set("message", randomMsg);
console.log("本次随机长消息: " + randomMsg.substring(0, 50) + "...");
```

### 版本 C：混合消息（综合压测用）

包含短、中、长三种类型，覆盖所有请求等级。

```javascript
// 前置操作 - 版本 C：混合消息（综合压测用）
var messages = [
    // === 短消息（L2_SIMPLE_LLM）===
    "我今天头痛",
    "最近总是失眠",
    "肚子疼怎么办",
    "咳嗽好几天了",
    "发烧38度",
    "经常头晕",
    "胃酸过多",
    "血压有点高",
    "嗓子疼怎么办",
    "皮肤过敏起红疹",
    // === 中等消息（L3_AGENT_MULTI）===
    "最近一周经常头痛，太阳穴位置跳着疼，下午加重，请问是什么问题？",
    "失眠两周了，每晚躺一小时才能睡着，半夜总醒，白天精神很差，怎么办？",
    "肚子疼三天了，肚脐周围隐痛，伴有腹泻，一天三四次，是不是吃坏东西了？",
    "咳嗽一周，干咳为主，晚上躺下咳得更厉害，影响睡眠，吃什么药好？",
    "我父亲65岁有高血压，最近血压150/95波动大，请问是否需要调整用药？",
    // === 长消息（L4_LONG_RUNNING）===
    "我母亲最近检查出甲状腺结节，B超显示大小约1.5厘米，边界清晰，TI-RADS分级3类。她今年55岁，已经绝经了。平时没什么不舒服的感觉，就是在体检时偶然发现的。医生说定期复查就行，但我们还是有点担心。请问这个风险大吗？需要注意什么？",
    "最近一个多月来，我的右膝盖在上下楼梯时会疼痛，走平路还好。没有受过外伤，就是每天走路比较多，大概一万步左右。按压膝盖内侧会有痛感，有时候膝盖会发出咔咔的响声。请问这是关节炎吗？需要做什么检查？",
    "我的胃反酸问题已经持续好几个月了，饭后尤其严重，有时候晚上躺下会感觉胃酸涌到喉咙，火辣辣的。之前做过胃镜，诊断是反流性食管炎，吃过奥美拉唑，停药后又复发了。长期吃这个药安全吗？有没有什么根治的方法？"
];
var randomMsg = messages[Math.floor(Math.random() * messages.length)];
pm.environment.set("message", randomMsg);
console.log("本次随机消息长度: " + randomMsg.length + "字");
if (randomMsg.length < 50) console.log("等级预计: L2_SIMPLE_LLM");
else if (randomMsg.length < 200) console.log("等级预计: L3_AGENT_MULTI");
else console.log("等级预计: L4_LONG_RUNNING");
```

### 配置步骤

1. 在 Apifox 中打开**同步 Chat** 或 **SSE 流式 Chat** 接口
2. 进入**前置操作** 选项卡 → 添加自定义脚本
3. 选择上面其中一个版本（A/B/C）粘贴代码
4. 保存后运行，每次请求 `{{message}}` 都会被替换为随机消息

> **建议**：同步 Chat 用版本 A（短消息，快速压测），SSE 用版本 B（长消息，测试流式稳定性），综合场景用版本 C。

---

## 六、压力测试方案

### 方案 A：降级模式测试（无 DASHSCOPE_API_KEY）

模拟 LLM 未配置时，验证系统不崩溃、降级正常：

| 阶段 | 接口 | 并发 | 时长 | 说明 |
|------|------|------|------|------|
| 1 | `POST /api/agent/chat` | 50 | 30s | 预热 |
| 2 | `POST /api/agent/chat` | 100 | 30s | 中等压力 |
| 3 | `POST /api/agent/chat` | 200 | 60s | 高压力 |
| 4 | `POST /api/agent/chat/async` | 100 | 30s | 异步提交 |
| 5 | 混合读写：chat(50%) + sessions(20%) + messages(20%) + pending(10%) | 150 | 120s | 综合负载 |

### 方案 B：全链路测试（配置 DASHSCOPE_API_KEY）

| 阶段 | 接口 | 并发 | 时长 | 说明 |
|------|------|------|------|------|
| 1 | `POST /api/agent/chat` | 5 | 30s | LLM 慢请求预热 |
| 2 | `POST /api/agent/chat` | 10 | 60s | 观察 LLM 响应时间 |
| 3 | `POST /api/agent/chat/async`（长消息触发 L3/L4） | 20 | 60s | 验证异步队列 |
| 4 | `GET /api/agent/task/{id}`（轮询） | 30 | 60s | 轮询压力 |
| 5 | SSE 流式 + 同步 Chat 混合 | 10 + 20 | 120s | 混合负载 |

### 方案 C：限流熔断验证

| 步骤 | 操作 | 预期 |
|------|------|------|
| 1 | 在 1s 内发送 15 个 `POST /api/agent/chat` | 第 1-10 个正常，第 11-15 个触发 `agentChat` 限流 |
| 2 | 观察日志中 `RateLimiter` 或 `RequestNotPermitted` | 限流日志出现 |

---

## 七、常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| `{{session_id}}` 为空，返回 500 | 后置操作未配置或脚本未执行 | 检查步骤2是否成功，查看后置操作控制台日志 |
| GET 请求返回 `Required request parameter 'userId' is not present` | userId 误放到 Body 中，而不是 Query | 在 Apifox 中改到 Params → Query 栏 |
| `{{task_id}}` 未填充 | 异步任务接口未配置后置操作 | 给步骤6添加后置脚本提取 taskId |
| 500 `userId 不能为空` | 创建会话的请求体用了 `user_id`（下划线）而不是 `userId`（驼峰） | 改成 `{"userId":"1"}` |
| `{"code":400,"message":"任务不存在或已过期"}` | taskId 为空或已过期 | 重新提交异步任务获取新 taskId |
