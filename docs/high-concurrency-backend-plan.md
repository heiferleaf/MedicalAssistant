# MedicalAssistant 后端高并发改造方案

## 1. 现状判断

当前后端是典型 Spring Boot MVC 单体服务：

- Web 层：`spring-boot-starter-web`，Servlet/Tomcat 阻塞模型。
- 数据层：MyBatis + JDBC + MySQL，未显式配置 Hikari 连接池参数。
- 缓存与锁：Redis + Redisson，已有部分读缓存、频率限制和分布式锁。
- 推送：Spring WebSocket，连接保存在单 JVM 内存 `WebSocketSessionManager`。
- 定时任务：`ThreadPoolTaskScheduler` + JVM 内存 `ScheduledFuture` 池。
- AI 能力：同步调用 Flask/RAG/Predict/LangChain4j，存在 30s 到 120s 级长耗时请求。

这个结构适合中小流量和单实例部署。要支撑高并发，核心问题不是简单调大线程数，而是把“长耗时、强状态、热点读、批量任务、跨实例推送”从请求线程和单 JVM 内存里拆出去。

## 2. 主要并发瓶颈

### 2.1 请求线程容易被长耗时调用耗尽

涉及模块：

- `FlaskAgentProxyService`
- `RagService`
- `PredictService`
- `FlaskRagProxyService`
- `AgentOrchestratorService`

当前 AI/RAG/预测调用基本都是同步阻塞，请求会一直占用 Tomcat 工作线程。`flask.timeout-ms` 默认 120000 ms，Agent 多轮调用默认 30000 ms。在并发稍高时，几十个慢请求就可能吃满 Web 线程，导致普通用药任务、登录、家庭组查询也被拖垮。

另外：

- `RagService` 和 `PredictService` 直接 `new RestTemplate()`，缺少连接池、统一超时、熔断、限流。
- `PredictService` 使用硬编码 `http://localhost:8001/api/predict/analyze`，不适合容器化和多环境。
- `FlaskClientConfig` 使用 `SimpleClientHttpRequestFactory`，没有 HTTP 连接池，无法复用连接。

### 2.2 单 JVM 状态阻碍水平扩容

涉及模块：

- `WebSocketSessionManager`
- `DynamicTaskScheduler`
- `WsAlarmBroadcastListener`

当前 WebSocket session 存在本机内存里。多实例部署后，A 实例收到业务事件，但用户 WebSocket 可能连在 B 实例，A 无法直接推送。

当前定时任务也存在本机内存里。多实例部署后，每个实例都会执行 `@PostConstruct` 和 `@Scheduled`，同一服药任务可能被重复调度、重复标记、重复推送。实例重启还会造成定时任务重建压力。

`DynamicTaskScheduler` 还有一个实现细节需要修复：创建提醒任务时变量是 `remindFuture`，但放入 `remindTaskPool` 的是 `future`，会导致取消提醒任务时取消到漏服任务的 future。

### 2.3 数据库读写热点和 N+1 查询

典型热点：

- 今日任务：`MedicationTaskMapper.findByUserIdAndDate`
- 历史任务：`MedicationTaskMapper.findHistory`
- 家庭健康快照：`FamilyGroupServiceImpl.getFamilyHealthSnapshot`
- Agent 会话：`AgentSessionController.getSessions`

当前已经有部分缓存，但仍有明显 N+1：

- `PlanServiceImpl.getPlanList` 收集药品 ID 后仍逐个 `medicineMapper.findById`。
- `TaskServiceImpl.batchQueryMedicines` 也是按药品 ID 循环查询。
- `getFamilyHealthSnapshot` 对每个家庭成员分别查最近健康数据、今日完成数、今日任务总数。
- `AgentSessionController.getSessions` 对每个 session 再查一次最近消息。

这些在单用户数据量小时不明显，但在家庭组、任务、Agent 消息增长后，会放大为数据库 QPS 和连接池压力。

### 2.4 索引与 SQL 需要面向访问模式重做

现有 `docker/init.sql` 已有一些基础索引，但高并发下还不够贴合查询。

建议重点补齐：

- `medication_task(status, task_date, time_point)`：支撑今日未完成任务扫描和调度。
- `medication_task(user_id, task_date, status)`：支撑今日任务、计数和状态筛选。
- `medication_task(plan_id, task_date)`：支撑删除未来任务。
- `medicine(user_id, name)` 唯一索引：防止 `findOrCreate` 并发创建重复药品。
- `health_data(user_id, is_deleted, measure_time)`：支撑最近健康数据。
- `family_member(user_id, status, is_deleted)`：支撑查询用户当前家庭组。
- `family_event_log(group_id, event_type, event_time)`：支撑今日告警查询。
- `agent_messages(session_id, id)` 已有，但 session 列表应避免逐 session 查询。

还要先修正一处一致性问题：`FamilyEventLogMapper.findDailyAlarms` 查询的是 `t_family_event_log`、`content`、`create_time`，而建表脚本是 `family_event_log`、`event_content`、`event_time`。这是正确性问题，压测前必须处理。

### 2.5 Redis 使用方式需要从“有缓存”升级到“抗击穿/抗雪崩”

当前已有缓存：

- 用户药品列表。
- 家庭成员 Hash。
- 家庭健康快照。
- 家庭告警列表。
- 在线成员状态。

问题点：

- Redis lettuce pool `max-active` 默认配置为 8，对高并发偏小。
- 热点缓存 TTL 固定，存在同一时间过期造成雪崩的风险。
- 查询缓存未统一处理空值缓存，容易被不存在数据打穿。
- 分布式锁等待时间有的达到 5 秒，会占住请求线程。
- 在线成员 Hash 没有 TTL/心跳续期，异常断线可能留下脏在线状态。

### 2.6 异步能力没有形成体系

`WsAlarmBroadcastListener` 使用了 `@Async`，但启动类没有 `@EnableAsync`，也没有业务线程池配置。即使启用默认异步线程池，也不应让 WebSocket 推送、AI 回调、消息写入、PDF 生成共用不受控线程池。

高并发下需要明确区分：

- Web 请求线程池。
- DB 连接池。
- Redis 连接池。
- AI 外部调用线程池或异步客户端。
- WebSocket 推送线程池。
- 定时/延迟任务线程池。

### 2.7 观测与保护能力不足

当前缺少：

- Spring Boot Actuator。
- Micrometer 指标。
- 慢 SQL 统计。
- HTTP 客户端耗时、超时、错误率指标。
- Redis 命中率指标。
- 业务限流、熔断、降级。
- 压测脚本和容量基线。

没有这些，调参容易靠感觉，无法判断瓶颈在 Tomcat、DB、Redis、Flask、LLM 还是业务锁。

## 3. 目标架构方向

### 3.1 应用层无状态化

目标：HTTP 服务可以水平扩容，任意实例都能处理任意请求。

改造点：

- JWT 鉴权继续保持无状态。
- WebSocket 连接状态从“本机 Map”升级为“实例路由 + Redis Pub/Sub 或消息队列广播”。
- 定时任务从“JVM 内存 ScheduledFuture”升级为“DB/Redis 延迟任务 + 分布式消费”。
- 本地文件输出如 `printpdf.output-dir` 不作为跨实例共享状态，改为对象存储或共享卷。

### 3.2 外部 AI 调用隔离

目标：AI 慢请求不能拖死核心业务接口。

推荐方向：

- 短期：所有 Flask/RAG/Predict 调用统一走一个带连接池的 HTTP client。
- 中期：为 AI 接口加 bulkhead、超时、重试、熔断、限流。
- 长期：对 Agent/RAG 使用异步任务模型，请求先返回 taskId，前端通过轮询/WebSocket 获取结果。

建议接口分层：

- 核心业务接口：登录、任务、药品、家庭组，要求低延迟、高可用。
- AI 辅助接口：允许排队、降级、限流。
- 管理/导出接口：PDF 等耗时操作异步化。

### 3.3 数据访问批量化和读缓存分层

目标：降低 MySQL QPS 和连接占用。

改造点：

- 新增批量查询 Mapper，例如 `findByIds`、`countByUsersAndDate`、`findLatestByUserIds`。
- 家庭健康快照一次 SQL 聚合，替代成员循环查。
- Agent session 列表一次 SQL 查出最后一条消息，替代循环查。
- Redis 缓存统一封装：空值缓存、随机 TTL、互斥锁、逻辑过期。
- 对特别热的只读数据引入 Caffeine 本地缓存，Redis 作为二级缓存。

### 3.4 延迟任务平台化

当前服药提醒、漏服标记、申请过期都是延迟任务。高并发和多实例下，建议从本机 scheduler 改为以下任一方案：

1. Redis ZSet 延迟队列
   - score 为执行时间戳。
   - 多实例用 Redisson lock 或 Lua 原子抢占。
   - 适合当前项目，改造成本较低。

2. MySQL 任务表轮询
   - 表字段包括 task_type、biz_id、execute_at、status、retry_count。
   - 使用 `SELECT ... FOR UPDATE SKIP LOCKED` 抢任务。
   - 可靠性比纯 Redis 好，吞吐略低。

3. 消息队列延迟消息
   - RabbitMQ 延迟插件、RocketMQ 延迟消息、Kafka 时间轮方案。
   - 适合后续服务拆分后使用。

推荐先做 Redis ZSet 或 MySQL 任务表，不建议继续为每个业务任务创建一个 JVM `ScheduledFuture`。

### 3.5 WebSocket 跨实例推送

目标：任意实例发布事件，在线用户都能收到。

推荐模型：

- Redis 中维护 `userId -> instanceId` 和在线心跳 TTL。
- 每个实例订阅自己的 Redis Pub/Sub channel，例如 `ws:node:{instanceId}`。
- 业务事件先根据 groupId 找用户，再按 userId 路由到对应实例 channel。
- 对每个 WebSocketSession 做串行发送，避免并发 send 同一 session。
- 用户离线时，关键消息落库为通知，避免只依赖在线推送。

## 4. 分阶段落地路线

### 阶段 0：压测前修正和基线

目标：先保证测出来的数据可信。

- 修正 `FamilyEventLogMapper.findDailyAlarms` 与建表字段不一致问题。
- 修正 `DynamicTaskScheduler` 中 `remindTaskPool.put(task.getId(), future)` 应使用 `remindFuture`。
- 关闭生产 SQL stdout：`mybatis.configuration.log-impl` 不应使用 `StdOutImpl`。
- 接入 Actuator + Micrometer，暴露 HTTP、JVM、Tomcat、Hikari、Redis 指标。
- 编写 JMeter/k6 压测脚本，覆盖登录、今日任务、更新任务状态、药品列表、家庭健康快照、Agent chat。

建议基线指标：

- 核心业务 P95 < 200 ms，P99 < 500 ms。
- Agent/RAG 单独统计，不和核心业务混算。
- HTTP 5xx < 0.1%。
- MySQL 连接池等待接近 0。
- Redis 命中率、慢命令、连接池等待可观测。

### 阶段 1：配置与连接池治理

目标：用低风险配置提升稳定性。

- 配置 Tomcat：
  - `server.tomcat.threads.max`
  - `server.tomcat.threads.min-spare`
  - `server.tomcat.accept-count`
  - `server.tomcat.max-connections`
- 配置 Hikari：
  - `spring.datasource.hikari.maximum-pool-size`
  - `minimum-idle`
  - `connection-timeout`
  - `max-lifetime`
  - `leak-detection-threshold`
- 配置 Redis pool：
  - `max-active`
  - `max-idle`
  - `min-idle`
  - `max-wait`
- 统一 HTTP client，使用连接池和明确的 connect/read timeout。
- 为 AI/RAG/Predict 设置独立并发上限，超过直接返回“系统繁忙/排队中”。

### 阶段 2：数据库与缓存优化

目标：把主要读热点降下来。

- 为药品、任务、家庭成员、健康数据、事件日志补齐组合索引。
- 给 `MedicineMapper` 增加 `findByIds`，替换循环查询。
- 给 `HealthDataMapper` 增加 `findLatestByUserIds`。
- 给 `MedicationTaskMapper` 增加按用户集合聚合今日任务计数的 SQL。
- 重写家庭健康快照：一次取成员、一次取健康数据、一次取任务统计。
- Agent session 列表改为一次 SQL 返回最后消息。
- Redis 缓存增加随机 TTL 和空值缓存，减少击穿。

### 阶段 3：异步化与削峰

目标：慢任务不占核心请求线程。

- 启用 `@EnableAsync`，配置命名线程池：
  - `wsPushExecutor`
  - `aiExecutor`
  - `pdfExecutor`
  - `domainEventExecutor`
- Agent/RAG/Predict 改成异步任务：
  - 请求入队返回 `taskId`。
  - 后台执行。
  - 结果写 DB/Redis。
  - WebSocket 或轮询返回结果。
- 对高频写操作使用业务幂等键，例如任务状态更新、创建计划、家庭申请审批。
- 对写后广播改为事件队列，避免事务内直接做推送。

### 阶段 4：多实例部署能力

目标：服务可以横向扩容。

- 定时任务改为 Redis ZSet/MySQL 任务表/消息队列延迟任务。
- WebSocket 改为跨实例路由推送。
- 在线状态加 TTL 和心跳续期。
- Spring Boot 多副本部署，前面挂 Nginx 或云负载均衡。
- WebSocket 可使用 sticky session 作为短期方案，但最终仍要做跨实例消息路由。
- PDF 等本地文件输出迁移到对象存储或共享存储。

### 阶段 5：韧性与治理

目标：局部故障不扩散。

- 引入 Resilience4j：
  - timeout
  - retry
  - circuit breaker
  - rate limiter
  - bulkhead
- 对 Flask/LLM 设置降级返回。
- 对 Redis 故障设置只读降级或 DB 回源保护。
- 对 MySQL 慢查询设置报警。
- 对业务锁等待时间设置上限，避免请求线程堆积。
- 建立容量评估表：单实例 QPS、DB QPS、Redis QPS、WebSocket 连接数、AI 并发数。

## 5. 建议优先级

优先做这些，收益最大、风险最低：

1. 修正明显正确性问题：事件日志 Mapper 字段、提醒定时 future。
2. 关闭 SQL stdout，接入 Actuator/Micrometer。
3. 配置 Tomcat、Hikari、Redis 连接池。
4. 统一外部 HTTP client，给 AI 调用加连接池、超时、并发隔离。
5. 补组合索引，替换药品和家庭快照的 N+1 查询。
6. 把 Agent/RAG/Predict 从同步请求改为异步任务。
7. 把 JVM 内存定时任务迁移到 Redis ZSet 或 MySQL 延迟任务表。
8. WebSocket 做跨实例 Pub/Sub 路由。

## 6. 推荐配置草案

以下数值不是最终答案，只能作为压测起点：

```yaml
server:
  tomcat:
    threads:
      max: 200
      min-spare: 20
    accept-count: 200
    max-connections: 10000

spring:
  datasource:
    hikari:
      maximum-pool-size: 30
      minimum-idle: 10
      connection-timeout: 1000
      max-lifetime: 1800000
      idle-timeout: 600000
      leak-detection-threshold: 3000
  data:
    redis:
      lettuce:
        pool:
          max-active: 64
          max-idle: 32
          min-idle: 8
          max-wait: 1000ms

mybatis:
  configuration:
    map-underscore-to-camel-case: true
    # 生产环境不要使用 StdOutImpl
```

AI 调用建议：

```yaml
flask:
  base-url: ${FLASK_URL:http://127.0.0.1:8001}
  connect-timeout-ms: 1000
  read-timeout-ms: 15000
  max-connections: 200
  max-connections-per-route: 50

agent:
  max-concurrency: 20
  queue-capacity: 200
```

## 7. 压测场景建议

核心业务场景：

- 登录 + 刷新 token。
- 查询今日任务。
- 更新任务状态。
- 查询药品列表。
- 创建用药计划。
- 查询家庭健康快照。
- 查询家庭告警。

AI 场景：

- Agent chat 短问题。
- RAG query。
- Predict analyze。
- AI 服务超时/不可用时的降级表现。

WebSocket 场景：

- 1k/5k/10k 在线连接。
- 家庭组事件广播。
- 同一用户多端连接。
- 实例重启后在线状态清理。

定时任务场景：

- 单日 10 万服药任务。
- 同一时间点大量提醒。
- 多实例同时抢延迟任务。
- 任务执行失败重试。

## 8. 结论

这个分支具备改造成高并发后端的基础：业务边界清楚，已有 Redis、Redisson、MyBatis、WebSocket 和调度模块。但当前实现仍偏单实例和同步阻塞模型。

最关键的改造方向是：

- 核心业务请求保持短路径。
- AI、PDF、推送、延迟任务全部异步化或隔离。
- 单 JVM 内存状态迁移到 Redis/DB/消息队列。
- 数据库访问批量化，索引按真实查询重建。
- 先建立指标和压测基线，再逐步调参和拆模块。

## 9. 基础设施层建设专项

当前主线任务应从“直接优化具体业务接口”转为“先补基础设施层”。原因是消息投递、hook、延迟任务、WebSocket 路由、缓存失效、AI 异步任务都会横跨多个业务模块。如果没有统一的基础设施抽象，后续拆微服务时会把耦合一起带出去。

### 9.1 当前可复用资产

项目里已经具备部分基础设施雏形：

- `spring-boot-starter-amqp` 已引入，`docker-compose.yml` 已包含 RabbitMQ。
- `CanalCacheConsumer` 已通过 RabbitMQ 消费 Canal binlog，用于缓存失效。
- `WsPubSubBroadcaster` 已用 Redis Pub/Sub 做跨实例 WebSocket 广播雏形。
- `ApplicationEventPublisher` 已用于家庭组、健康数据、服药任务事件。
- `RedisService` 和 Redisson 已用于缓存、分布式锁和幂等控制。

但这些能力目前是分散的：业务事件、缓存事件、推送事件、延迟任务事件没有统一事件模型；`@Async` 没有启用和隔离线程池；RabbitMQ 只服务 Canal 缓存消费，还没有承载业务事件削峰。

### 9.2 基础设施层目标

基础层建设的目标不是一次性把单体拆成多个服务，而是先把单体内部变成“模块化单体 + 统一基础设施”。这样既能快速降低并发风险，也为后续微服务拆分保留清晰边界。

目标能力：

- 统一业务事件总线：业务代码只发布领域事件，不直接调用推送、短信、缓存刷新、AI 后处理。
- 统一消息队列封装：RabbitMQ 负责削峰、重试、死信、异步消费。
- 统一 hook 机制：在业务动作完成后触发扩展点，例如任务状态变更后触发缓存失效、家庭告警、WebSocket 推送、后续通知。
- 统一延迟任务平台：服药提醒、漏服标记、邀请过期不再依赖单 JVM `ScheduledFuture`。
- 统一异步线程池：WebSocket 推送、AI 调用、PDF 生成、领域事件消费互相隔离。
- 统一观测指标：消息积压、消费失败、重试次数、事件耗时、线程池队列长度可观测。

### 9.3 建议包结构

建议先在单体内建立稳定包结构，后续拆服务时按这些边界迁移：

```text
common/infra/
  async/              # 线程池、@Async executor、任务装饰器
  event/              # DomainEvent、EventPublisher、EventSubscriber
  mq/                 # RabbitMQ exchange/queue/routing 封装
  hook/               # Hook 定义、注册、执行链
  delay/              # 延迟任务接口、Redis ZSet 或 RabbitMQ 延迟队列实现
  idempotency/        # 幂等键、去重表、Redis 去重
  observability/      # Actuator/Micrometer、自定义指标
  resilience/         # 熔断、限流、bulkhead、重试
```

业务模块只依赖 `common/infra` 暴露的接口，不直接依赖 RabbitMQ、Redis Pub/Sub、线程池实现细节。

### 9.4 事件总线与消息队列模型

统一事件模型：

```text
DomainEvent
  eventId             # 全局唯一 ID，用于幂等
  eventType           # 例如 medication.task.status.changed
  aggregateType       # task / plan / family / health / agent
  aggregateId
  userId
  groupId
  occurredAt
  traceId
  payload
```

推荐 RabbitMQ 拓扑：

```text
exchange: medical.domain.topic
  medication.task.*          -> queue.medication.task
  family.*                   -> queue.family.event
  health.*                   -> queue.health.event
  ws.push.*                  -> queue.ws.push
  ai.task.*                  -> queue.ai.task
  cache.invalidate.*         -> queue.cache.invalidate

exchange: medical.delay.topic
  medication.remind          -> queue.delay.medication.remind
  medication.missed          -> queue.delay.medication.missed
  family.invite.expire       -> queue.delay.family.invite

exchange: medical.dlx.topic
  # 所有失败超过阈值的消息进入死信队列，供人工排查和补偿
```

短期可以继续保留 Canal 消费缓存失效，但业务写操作应逐步发布领域事件。Canal 更适合兜底一致性和跨系统同步，不适合作为全部业务 hook 的唯一来源，因为它缺少业务语义。

### 9.5 Hook 机制设计

hook 负责把“业务动作之后要做什么”标准化，避免 Service 中直接塞入推送、缓存、日志、通知逻辑。

建议定义以下扩展点：

- `MedicationTaskStatusChangedHook`
  - 删除家庭快照缓存。
  - 需要时写入家庭告警日志。
  - 发布 `ws.push.medication`。
  - 取消或更新延迟任务。
- `MedicationPlanChangedHook`
  - 重建未来任务。
  - 删除药品/任务相关缓存。
  - 发布计划变更推送。
- `FamilyMemberChangedHook`
  - 同步家庭成员缓存。
  - 删除家庭快照和告警缓存。
  - 发布成员变更推送。
- `HealthDataChangedHook`
  - 删除健康数据缓存和家庭快照缓存。
  - 发布健康数据变更推送。
- `AgentTaskCompletedHook`
  - 写入会话消息。
  - 发布 AI 任务完成推送。
  - 记录耗时和 token 等指标。

hook 执行策略：

- 同事务内只做必要校验和 outbox 记录。
- 事务提交后再异步执行 hook，避免推送或外部调用拖慢主事务。
- 每个 hook 使用 `eventId + hookName` 做幂等。
- hook 失败进入重试队列，超过阈值进入死信队列。

### 9.6 延迟任务改造

服药提醒、漏服标记、邀请过期应从 JVM 内存调度迁移到统一延迟任务平台。

短期推荐两种方案之一：

1. Redis ZSet 延迟队列
   - `delay:ready:{taskType}` 保存待执行任务。
   - score 为执行时间戳。
   - 消费者用 Lua 或 Redisson lock 原子抢占。
   - 适合当前项目快速落地。

2. RabbitMQ 延迟队列
   - 使用 TTL + DLX 或 delayed-message 插件。
   - 与后续 MQ 体系一致。
   - 需要确认部署环境是否允许启用插件。

任务体字段：

```text
DelayTask
  taskId
  taskType
  bizId
  executeAt
  retryCount
  maxRetry
  payload
```

迁移顺序：

1. 保留 `DynamicTaskScheduler` 业务处理方法，但抽出 `MedicationReminderJob`、`MedicationMissedJob`、`FamilyInviteExpireJob`。
2. 新增 `DelayTaskPublisher`，创建计划或邀请时只写延迟任务。
3. 新增 `DelayTaskConsumer`，由多实例竞争消费。
4. 移除 `medicationTaskPool`、`remindTaskPool`、`inviteTaskPool` 这些本机 Map。

### 9.7 WebSocket 基础设施改造

当前 Redis Pub/Sub 是可行雏形，但应从 `ws:group:*` 广播升级为明确的用户路由。

建议模型：

- Redis 维护 `ws:route:user:{userId} -> instanceId`，设置 TTL。
- Redis 维护 `ws:online:user:{userId}` 心跳 TTL，不再使用无 TTL 的在线 Hash。
- 每个实例订阅 `ws:node:{instanceId}`。
- 业务只发布 `WsPushCommand` 到 MQ 或 Redis，基础设施层负责路由到实例。
- 本地同一用户允许多端连接，`WebSocketSessionManager` 改为 `userId -> sessionId set`。
- 对同一个 session 串行发送，避免并发 `sendMessage`。

短期实现可以继续用 Redis Pub/Sub；中期应让业务事件先进入 RabbitMQ 的 `queue.ws.push`，再由 ws-push 消费者做路由和发送。

### 9.8 微服务拆分路线

不建议现在直接物理拆服务。应先按模块边界完成“模块化单体”，再按流量和故障隔离需求拆出服务。

建议最终服务边界：

- `user-service`
  - 用户、登录、JWT、刷新 token。
- `medication-service`
  - 药品、计划、服药任务、漏服标记。
- `family-service`
  - 家庭组、成员、邀请申请、家庭事件日志。
- `health-service`
  - 健康数据、家庭健康快照。
- `agent-service`
  - Agent 会话、RAG、Predict、OCR、工具调用。
- `notification-service`
  - WebSocket、短信、邮件、站内通知。
- `scheduler-service`
  - 延迟任务、补偿任务、批处理任务。
- `gateway-service`
  - 鉴权、限流、路由、灰度。

拆分顺序：

1. 先拆 `agent-service`，因为它慢、重、失败率高，最需要与核心业务隔离。
2. 再拆 `notification-service`，集中处理 WebSocket 和外部通知。
3. 再拆 `scheduler-service`，避免所有业务实例都承担延迟任务。
4. 最后拆 `medication/family/health/user`，这些模块数据关联更强，应等事件和一致性机制稳定后再拆。

拆分前置条件：

- 所有跨模块调用先改为接口或事件，不直接跨包读写实现类。
- 所有跨模块数据变更有领域事件。
- 每个消费者有幂等和死信处理。
- 关键链路有 traceId 和指标。
- 数据库表可以先共库不同 schema，后续再独立数据库。

### 9.9 优先落地清单

第一批基础设施任务：

1. 启用 `@EnableAsync`，新增 `AsyncConfig`，配置 `domainEventExecutor`、`wsPushExecutor`、`aiExecutor`、`pdfExecutor`。
2. 新增统一 `DomainEvent`、`DomainEventPublisher`、`DomainEventHandler`。
3. 新增 RabbitMQ 基础配置：domain exchange、delay exchange、DLX、JSON message converter。
4. 把 `WsAlarmBroadcastListener` 从本地 `@EventListener` 逐步迁移为 `ws.push` 队列消费者。
5. 新增 hook 执行框架，先接入任务状态变更、家庭成员变更、健康数据变更三个高价值 hook。
6. 新增延迟任务发布/消费接口，先替换服药提醒和漏服标记。
7. 为 MQ 消费增加幂等表或 Redis 幂等键。
8. 接入 Actuator/Micrometer，增加 MQ、线程池、延迟任务积压指标。

第二批基础设施任务：

1. Agent/RAG/Predict 改为异步任务模型，RabbitMQ 入队，返回 `taskId`。
2. WebSocket 在线状态改为带 TTL 的用户路由表。
3. 建立 outbox 表，保证“数据库提交成功”和“事件最终发布”一致。
4. 建立死信补偿接口和后台管理查询。
5. 按模块边界收敛依赖，为后续拆服务做准备。

### 9.10 验收标准

基础设施层第一阶段完成后，应满足：

- 核心业务 Service 不直接调用 WebSocket 推送实现。
- 核心业务 Service 不直接创建本机延迟定时器。
- 至少任务状态变更、家庭成员变更、健康数据变更三类事件走统一事件模型。
- RabbitMQ 有业务事件队列、延迟任务队列、死信队列。
- 消费者具备幂等、重试、失败日志。
- 多实例部署时，同一延迟任务只会被一个实例消费。
- 任意实例发布推送事件，在线用户所在实例都能收到并发送。
- 可以看到线程池队列长度、MQ 积压、消费失败、延迟任务积压等指标。
