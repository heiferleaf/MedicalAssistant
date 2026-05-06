# 微服务模式 WebSocket 推送修复说明

## 背景

当前 `docker-compose.microservices.yml` 会启动 7 个后端业务容器，并通过 Nginx 按路径转发：

- `/api/user/*` -> `user-service:8081`
- `/api/plan/*`、`/api/task/*`、`/api/medicine/*` -> `medication-service:8082`
- `/api/family/*` -> `family-service:8083`
- `/api/health/*` -> `health-service:8084`
- `/api/agent/*`、`/api/rag/*`、`/api/predict/*`、`/api/ocr/*` -> `agent-service:8085`
- `/ws` -> `notification-service:8086`

这个拆分目前更接近“按入口角色拆分的多实例部署”，还不是严格意义上的微服务。部分服务仍然会直接扫描其他领域的 Mapper，例如 `medication-service` 会直接使用 `FamilyMemberMapper`、`UserMapper`，所以服务间数据访问仍然发生在同一个 JVM 内，而不是通过 HTTP/RPC 调用。

## 原问题

原来的 WebSocket 推送链路依赖 Spring `ApplicationEvent`：

1. `medication-service`、`health-service`、`family-service` 等业务容器发布本地 `ApplicationEvent`。
2. `WsAlarmBroadcastListener` 通过 `@EventListener` 监听事件。
3. Listener 直接使用本 JVM 内的 `WebSocketSessionManager` 推送消息。

这个方案在单体应用中可以工作，但在微服务模式下有两个问题：

- Spring `ApplicationEvent` 只在当前 JVM 内传播，不能跨容器传到 `notification-service`。
- WebSocket 连接都由 Nginx 转发到 `notification-service`，会话只存在 `notification-service` 的内存里，其他业务容器找不到用户会话。

结果是：业务容器产生的家庭组更新、健康数据更新、服药提醒等事件不会可靠推送到已连接的 WebSocket 客户端。

## 修复方案

本次修复将“本地事件”桥接到 RabbitMQ，再由 `notification-service` 统一推送 WebSocket。

新的消息流：

```text
业务服务
  publishEvent(FamilyPushEvent / UserTaskMedicineRemindEvent)
        |
        v
ApplicationEventWsPushBridge
  AFTER_COMMIT 后转换为 WsPushCommand
        |
        v
RabbitMQ exchange: medical.domain.topic
  routing key: ws.push.user
        |
        v
RabbitMQ queue: queue.ws.push
        |
        v
notification-service
  WsPushMessageListener 消费 MQ
        |
        v
WsPubSubBroadcaster
  本机存在 WebSocket session 则直接发送
  多 notification 实例场景下可继续通过 Redis Pub/Sub 转发
```

关键变化：

- 新增 `ApplicationEventWsPushBridge`，放在 `common.infra.push`，所有服务都会扫描到。
- 家庭组事件会在事务提交后查询家庭组活跃成员，并为每个成员发送一条 `WsPushCommand`。
- 单用户提醒事件会直接发送给目标用户。
- 删除旧的本地 `WsAlarmBroadcastListener`，避免本地推送和 MQ 推送重复执行。
- 只有 `service.websocket.enabled=true` 的服务会消费 `queue.ws.push`。在当前配置中，只有 `notification-service` 开启 WebSocket。

## 连接与资源说明

每个业务容器都会各自创建数据库、Redis、RabbitMQ 连接，这是 Spring Boot 多进程部署的正常行为。只要 Compose 中的环境变量正确，容器就能连接到同一个基础设施：

- MySQL：`MYSQL_HOST=mysql`、`MYSQL_PORT=3306`
- Redis：`REDIS_HOST=redis`
- RabbitMQ：`SPRING_RABBITMQ_HOST=rabbitmq`、`SPRING_RABBITMQ_PORT=5672`

需要注意的是，7 个业务容器会重复加载不少 Bean，也会带来更多连接池和内存开销。对当前代码规模来说，如果目标不是展示微服务部署形态，单体后端加一个基础设施栈会更省资源；如果目标是验证微服务拆分，则必须保证跨容器通信走 MQ/HTTP/RPC，不能依赖本地内存事件。

## 验证步骤

在项目根目录执行：

```bash
docker compose -f docker-compose.microservices.yml up -d --build
```

确认容器状态：

```bash
docker compose -f docker-compose.microservices.yml ps
```

确认 `notification-service` 已监听 WebSocket 推送队列：

```bash
docker compose -f docker-compose.microservices.yml logs -f notification-service
```

触发以下任一业务动作后，观察 `notification-service` 日志是否出现 `WebSocket 推送` 或 `Pub/Sub` 相关日志：

- 创建或更新家庭成员关系。
- 同步健康数据：`/api/health/sync`。
- 更新服药任务状态：`/api/task/{taskId}/status`。
- 定时任务触发服药提醒或漏服提醒。

如果要进一步确认 RabbitMQ 队列，可访问管理页面：

```text
http://localhost:15672
```

默认账号密码：

```text
guest / guest
```

重点检查：

- exchange：`medical.domain.topic`
- queue：`queue.ws.push`
- binding：`ws.push.*`
- 消费者：应来自 `notification-service`

## 结论

修复后，业务容器不再需要持有 WebSocket 会话，也不依赖本地 JVM 事件跨服务传播。业务服务只负责发布本地业务事件，公共桥接器负责投递 RabbitMQ，`notification-service` 负责统一推送到真实 WebSocket 连接。
